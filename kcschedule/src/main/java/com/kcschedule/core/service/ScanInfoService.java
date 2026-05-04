package com.kcschedule.core.service;

import cn.hutool.core.date.DateUtil;
import com.kcschedule.core.annotation.KCScheduled;
import com.kcschedule.core.config.KCScheduleConfig;
import com.kcschedule.core.utils.CronParser;
import com.kcschedule.core.vo.ScheduledInfoVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.*;

@Component
public class ScanInfoService implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ScanService scanService;

    @Autowired
    private KCScheduleConfig scheduleConfig;

    private static final Logger log = LoggerFactory.getLogger(ScanInfoService.class);

    private String resourcePattern = "**/*.class";
    private ResourcePatternResolver resourcePatternResolver;
    private MetadataReaderFactory metadataReaderFactory;
    private ApplicationContext applicationContext;

    private void scan(String basePackage) {
        String packageSearchPath = "classpath*:" + resolveBasePackage(basePackage) + "/" + this.resourcePattern;
        try {
            Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                    AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
                    Class<?> clazz = findClazz(metadataReader.getClassMetadata().getClassName());
                    if (clazz != null) {
                        parseAnnotatedMethods(annotationMetadata, clazz);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning classes: " + e.getMessage());
        }
    }

    private Class<?> findClazz(String clazzName) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(clazzName);
            Object bean = applicationContext.getBean(clazz);
            if (bean != null) return clazz;
        } catch (NoUniqueBeanDefinitionException e) {
            return clazz;
        } catch (ClassNotFoundException | NoSuchBeanDefinitionException e) {
            return null;
        } catch (Exception e) {
            log.warn("Error parsing class: " + e.getMessage());
        }
        return null;
    }

    private void parseAnnotatedMethods(AnnotationMetadata annotationMetadata, Class<?> clazz) {
        Set<MethodMetadata> methodMetadatas = annotationMetadata.getAnnotatedMethods(KCScheduled.class.getName());
        for (MethodMetadata methodMetadata : methodMetadatas) {
            String classPath = clazz.getName();
            String simpleName = clazz.getSimpleName();
            String methodName = methodMetadata.getMethodName();
            Map<String, Object> attrs = methodMetadata.getAnnotationAttributes(KCScheduled.class.getName());
            String key = simpleName + methodName;

            ScheduledInfoVo latestInfo = new ScheduledInfoVo();
            latestInfo.setScheduledName((String) attrs.get("cronName"));
            String cron = (String) attrs.get("cron");
            latestInfo.setCron(cron);
            latestInfo.setDescribe((String) attrs.get("cronDocs"));
            latestInfo.setIsRun((boolean) attrs.get("isRun"));
            if (attrs.get("dirName") != null) {
                latestInfo.setDirName(String.valueOf(attrs.get("dirName")));
            }
            String nextDate = CronParser.getNextExecution(cron, new Date());
            latestInfo.setNextDate(nextDate);
            latestInfo.setClassName(classPath);
            latestInfo.setFunctionName(methodName);
            latestInfo.setFlag("success");
            latestInfo.setRunDate(DateUtil.now());

            if (!scanService.isTask(key)) {
                LinkedList<ScheduledInfoVo> list = new LinkedList<>();
                list = scanService.addTaskToList(list, latestInfo);
                scanService.setScheduledInfoVo(key, list);
                log.info("Registered task: {} - {}", key, latestInfo.getScheduledName());
            } else {
                LinkedList<ScheduledInfoVo> existingTasks = scanService.getScheduledInfoVo(key);
                if (!existingTasks.isEmpty()) {
                    ScheduledInfoVo lastTask = existingTasks.getLast();
                    boolean needUpdate = !Objects.equals(lastTask.getScheduledName(), latestInfo.getScheduledName()) ||
                            !Objects.equals(lastTask.getCron(), latestInfo.getCron()) ||
                            !Objects.equals(lastTask.getDescribe(), latestInfo.getDescribe()) ||
                            !Objects.equals(lastTask.getDirName(), latestInfo.getDirName()) ||
                            lastTask.getIsRun() != latestInfo.getIsRun();
                    if (needUpdate) {
                        log.info("Task config changed: {} - {}", key, latestInfo.getScheduledName());
                        LinkedList<ScheduledInfoVo> updatedTasks = new LinkedList<>();
                        for (ScheduledInfoVo existingTask : existingTasks) {
                            updatedTasks.add(mergeTaskInfo(existingTask, latestInfo));
                        }
                        latestInfo.setRunDate(DateUtil.now());
                        updatedTasks = scanService.addTaskToList(updatedTasks, latestInfo);
                        scanService.setScheduledInfoVo(key, updatedTasks);
                    }
                }
            }
        }
    }

    private static ScheduledInfoVo mergeTaskInfo(ScheduledInfoVo existing, ScheduledInfoVo latest) {
        ScheduledInfoVo updated = new ScheduledInfoVo();
        updated.setScheduledName(latest.getScheduledName());
        updated.setCron(latest.getCron());
        updated.setDescribe(latest.getDescribe());
        updated.setDirName(latest.getDirName());
        updated.setIsRun(latest.getIsRun());
        updated.setClassName(latest.getClassName());
        updated.setFunctionName(latest.getFunctionName());
        updated.setFlag(existing.getFlag());
        updated.setNextDate(existing.getNextDate());
        updated.setRunDate(existing.getRunDate());
        updated.setRunTime(existing.getRunTime());
        updated.setErrorMsg(existing.getErrorMsg());
        return updated;
    }

    protected String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(basePackage);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        this.applicationContext = ctx;
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(ctx);
        try {
            this.metadataReaderFactory = new CachingMetadataReaderFactory(ctx);
        } catch (Exception e) {
            this.metadataReaderFactory = new CachingMetadataReaderFactory();
        }

        String basePkg = scheduleConfig.getScanBasePackage();
        if (basePkg == null || basePkg.trim().isEmpty()) {
            basePkg = ctx.getBeanNamesForType(Object.class).length > 0 ?
                    getMainBasePackage(ctx) : "";
        }
        if (!basePkg.trim().isEmpty()) {
            scan(basePkg);
        }
    }

    private String getMainBasePackage(ApplicationContext ctx) {
        String[] names = ctx.getBeanDefinitionNames();
        for (String name : names) {
            try {
                Class<?> beanType = ctx.getType(name);
                if (beanType == null) continue;
                String beanClassName = beanType.getName();
                if (beanClassName.startsWith("org.springframework.")
                        || beanClassName.startsWith("javax.")
                        || beanClassName.startsWith("jakarta.")) continue;
                int lastDot = beanClassName.lastIndexOf('.');
                if (lastDot > 0) {
                    return beanClassName.substring(0, lastDot);
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public String refreshScheduledTasks() {
        try {
            log.info("Refreshing scheduled tasks...");
            String basePkg = scheduleConfig.getScanBasePackage();
            if (basePkg == null || basePkg.trim().isEmpty()) {
                basePkg = getMainBasePackage(applicationContext);
            }
            if (!basePkg.trim().isEmpty()) {
                scan(basePkg);
            }
            log.info("Scheduled tasks refreshed");
            return "Scheduled tasks refreshed";
        } catch (Exception e) {
            String errorMsg = "Failed to refresh: " + e.getMessage();
            log.error(errorMsg, e);
            return errorMsg;
        }
    }
}
