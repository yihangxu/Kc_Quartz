package com.kcschedule.core.aop;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.kcschedule.core.annotation.ServiceLog;
import com.kcschedule.core.dao.LogPersistDao;
import com.kcschedule.core.msg.NotifyService;
import com.kcschedule.core.vo.MethodLogDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Aspect
@Component
public class MonitorLogAspect {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("kcscheduleExecutor")
    private Executor asyncExecutor;
    @Autowired
    private NotifyService notifyService;
    @Autowired(required = false)
    private LogPersistDao logPersistDao;

    @Around("@annotation(com.kcschedule.core.annotation.ServiceLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        ServiceLog annotation = method.getAnnotation(ServiceLog.class);
        if (annotation == null) return joinPoint.proceed();
        return doAround(joinPoint, annotation);
    }

    private Object doAround(ProceedingJoinPoint joinPoint, ServiceLog logAnno) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        LocalDateTime startTime = LocalDateTime.now();
        long start = System.currentTimeMillis();
        int success = 0;
        String paramsJson = "", resultJson = "", errorMsg = "";

        try {
            if (logAnno.logParams()) {
                paramsJson = toJsonLogParams(joinPoint.getArgs(), logAnno.sensitiveFields());
            }
            Object result = joinPoint.proceed();
            if (logAnno.logResult()) {
                resultJson = toJsonWithMasking(result, logAnno.sensitiveFields());
            }
            return result;
        } catch (Exception ex) {
            success = 1;
            errorMsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            String finalErrorMsg = errorMsg;
            asyncExecutor.execute(() -> {
                String error = "Service task error, name: " + logAnno.value()
                        + ", error: " + finalErrorMsg
                        + ", method: " + methodName
                        + ", time: " + startTime;
                notifyService.sendErrorMsg(error);
            });
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            MethodLogDTO logDto = MethodLogDTO.builder()
                    .methodName(methodName)
                    .description(logAnno.value())
                    .success(success)
                    .duration(duration)
                    .params(paramsJson)
                    .result(resultJson)
                    .error(errorMsg)
                    .timestamp(startTime)
                    .build();

            asyncExecutor.execute(() -> {
                try {
                    if (logPersistDao != null) {
                        logPersistDao.save(logDto);
                    }
                } catch (Exception e) {
                    logger.warn("Log write failed", e);
                }
            });
        }
    }

    private String toJsonLogParams(Object[] data, String[] sensitiveFields) {
        JSONArray jsonArray = new JSONArray();
        if (data != null && data.length > 0) {
            for (Object datum : data) {
                jsonArray.add(preProcessObject(datum));
            }
        }
        String jsonStr = jsonArray.toString();
        return maskSensitiveFields(jsonStr, sensitiveFields);
    }

    private Object preProcessObject(Object obj) {
        if (obj instanceof MultipartFile) {
            MultipartFile file = (MultipartFile) obj;
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("type", "MultipartFile");
            fileInfo.put("originalFilename", file.getOriginalFilename());
            fileInfo.put("size", file.getSize());
            fileInfo.put("contentType", file.getContentType());
            return fileInfo;
        } else if (obj instanceof List) {
            return ((List<?>) obj).stream().map(this::preProcessObject).collect(Collectors.toList());
        } else if (obj != null && obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            Object[] newArray = new Object[length];
            for (int i = 0; i < length; i++) {
                newArray[i] = preProcessObject(Array.get(obj, i));
            }
            return newArray;
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Map<Object, Object> newMap = new HashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                newMap.put(entry.getKey(), preProcessObject(entry.getValue()));
            }
            return newMap;
        }
        return obj;
    }

    private String toJsonWithMasking(Object data, String[] sensitiveFields) {
        try {
            String jsonStr = JSONUtil.toJsonStr(data);
            return maskSensitiveFields(jsonStr, sensitiveFields);
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    private String maskSensitiveFields(String jsonStr, String[] sensitiveFields) {
        for (String field : sensitiveFields) {
            String pattern = "(\"" + Pattern.quote(field) + "\":\\s*\")[^\"]*\"";
            jsonStr = jsonStr.replaceAll(pattern, "$1***\"");
        }
        return jsonStr;
    }
}
