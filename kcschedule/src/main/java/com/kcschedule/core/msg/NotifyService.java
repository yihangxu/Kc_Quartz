package com.kcschedule.core.msg;

import com.kcschedule.core.config.KCScheduleConfig;
import com.kcschedule.core.dao.LogPersistDao;
import com.kcschedule.core.vo.MethodLogDTO;
import com.kcschedule.core.vo.MiniUIPageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class NotifyService implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private List<NotifyChannel> channelList;

    @Autowired
    private KCScheduleConfig scheduleConfig;

    @Autowired(required = false)
    private LogPersistDao logPersistDao;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean running = true;

    public void sendErrorMsg(String message) {
        redisTemplate.opsForList().rightPush(scheduleConfig.getQueueName(), message);
    }

    private void handleMessage(Object message) {
        if (channelList == null) return;
        for (NotifyChannel channel : channelList) {
            if (channel.isEnabled()) {
                channel.send(message.toString());
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        executorService.execute(this::consumeMessages);
    }

    private void consumeMessages() {
        while (running) {
            try {
                Object message = redisTemplate.opsForList().leftPop(scheduleConfig.getQueueName(), 30, TimeUnit.SECONDS);
                if (message != null) {
                    handleMessage(message);
                }
            } catch (Exception e) {
                System.err.println("Message consume error: " + e.getMessage());
            }
        }
    }

    @Override
    public void destroy() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public MiniUIPageInfo<MethodLogDTO> scanLogPage(MethodLogDTO query) {
        if (logPersistDao == null) {
            MiniUIPageInfo<MethodLogDTO> empty = new MiniUIPageInfo<>();
            empty.setData(new java.util.ArrayList<>());
            empty.setTotal(0);
            return empty;
        }

        int pageNum = (query.getPageIndex() == null || query.getPageIndex() < 0) ? 0 : query.getPageIndex();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        int offset = pageNum * pageSize;
        query.setOffset(offset);

        List<MethodLogDTO> dataList = logPersistDao.queryLogPage(query);
        int total = logPersistDao.countLog(query);

        MiniUIPageInfo<MethodLogDTO> pageInfo = new MiniUIPageInfo<>();
        pageInfo.setData(dataList);
        pageInfo.setTotal(total);
        return pageInfo;
    }
}
