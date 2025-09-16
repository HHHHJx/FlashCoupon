package com.jx.flashcoupon.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.engine.mq.util.MessageRetryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 延迟重试消息处理器
 * 负责从Redis延迟队列中获取任务并执行重试
 * 开发时间：2025-10-15
 */
@Component
@RequiredArgsConstructor
@Slf4j(topic = "DelayedRetryMessageProcessor")
public class DelayedRetryMessageProcessor implements ApplicationRunner {

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageRetryUtil messageRetryUtil;

    private static final String DELAY_QUEUE_KEY = "mq:retry:delay_queue";
    private static final String RETRY_TASK_KEY_PREFIX = "mq:retry:";
    private static final int MAX_BATCH_SIZE = 100; // 每次处理的最大消息数

    @Override
    public void run(ApplicationArguments args) {
        // 启动后台线程定期检查延迟队列
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "delayed-retry-message-processor");
            thread.setDaemon(true);
            return thread;
        });
        
        // 每10秒检查一次延迟队列
        executorService.scheduleAtFixedRate(this::processDelayedTasks, 0, 10, TimeUnit.SECONDS);
        
        log.info("[延迟重试消息处理器] 已启动，每10秒检查一次延迟队列");
    }
    
    /**
     * 处理延迟任务
     */
    private void processDelayedTasks() {
        try {
            // 获取当前时间戳
            long now = System.currentTimeMillis();
            
            // 获取所有已到期的任务
            Set<String> taskKeys = stringRedisTemplate.opsForZSet().rangeByScore(DELAY_QUEUE_KEY, 0, now, 0, MAX_BATCH_SIZE);
            
            if (taskKeys == null || taskKeys.isEmpty()) {
                return;
            }
            
            for (String taskKey : taskKeys) {
                try {
                    // 从Redis中获取任务详情
                    String messageJson = stringRedisTemplate.opsForValue().get(taskKey);
                    
                    if (messageJson == null) {
                        // 任务已不存在，从延迟队列中移除
                        stringRedisTemplate.opsForZSet().remove(DELAY_QUEUE_KEY, taskKey);
                        continue;
                    }
                    
                    // 解析任务信息，这里需要根据实际存储的内容进行调整
                    // 实际项目中，应该在存储时就包含目标主题、消息内容和重试配置
                    Map<String, Object> taskInfo = JSON.parseObject(messageJson, Map.class);
                    String destination = (String) taskInfo.get("destination");
                    String messageKey = (String) taskInfo.get("messageKey");
                    Object payload = taskInfo.get("payload");
                    int maxRetryCount = ((Number) taskInfo.getOrDefault("maxRetryCount", 3)).intValue();
                    
                    // 构建消息对象
                    Message<?> message = new GenericMessage<>(payload);
                    
                    // 执行重试
                    boolean success = messageRetryUtil.retrySendMessage(
                            destination, 
                            message, 
                            messageKey, 
                            maxRetryCount, 
                            60 // 重试间隔60秒
                    );
                    
                    // 无论成功失败，都从延迟队列中移除
                    stringRedisTemplate.opsForZSet().remove(DELAY_QUEUE_KEY, taskKey);
                    
                    if (!success) {
                        // 如果重试失败，记录日志
                        log.warn("[延迟重试] 消息重试失败，任务Key：{}", taskKey);
                    }
                } catch (Exception e) {
                    log.error("[延迟重试] 处理任务异常，任务Key：{}", taskKey, e);
                }
            }
        } catch (Exception e) {
            log.error("[延迟重试] 处理延迟队列异常", e);
        }
    }
}