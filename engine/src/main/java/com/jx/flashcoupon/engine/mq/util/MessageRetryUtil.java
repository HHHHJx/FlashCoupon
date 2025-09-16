package com.jx.flashcoupon.engine.mq.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 消息重试工具类
 * 提供消息发送失败的重试机制
 * 开发时间：2025-10-15
 */
@Component
@RequiredArgsConstructor
@Slf4j(topic = "MessageRetryUtil")
public class MessageRetryUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 重试发送消息
     * @param destination 目标主题
     * @param message 消息内容
     * @param messageKey 消息唯一标识
     * @param maxRetryCount 最大重试次数
     * @param retryIntervalSeconds 重试间隔(秒)
     * @return 是否发送成功
     */
    public boolean retrySendMessage(String destination, Message<?> message, String messageKey, int maxRetryCount, long retryIntervalSeconds) {
        // 检查是否已经达到最大重试次数
        String retryKey = buildRetryKey(destination, messageKey);
        String retryCountStr = stringRedisTemplate.opsForValue().get(retryKey);
        int currentRetryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;
        
        if (currentRetryCount >= maxRetryCount) {
            log.error("[消息重试] 消息已达到最大重试次数 {}，放弃重试。目标主题：{}，消息Key：{}", 
                    maxRetryCount, destination, messageKey);
            return false;
        }
        
        // 增加重试计数
        currentRetryCount++;
        stringRedisTemplate.opsForValue().set(retryKey, String.valueOf(currentRetryCount), retryIntervalSeconds * maxRetryCount, TimeUnit.SECONDS);
        
        try {
            // 执行消息发送
            SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
            boolean success = "SEND_OK".equals(sendResult.getSendStatus().name());
            
            if (success) {
                // 发送成功，删除重试记录
                stringRedisTemplate.delete(retryKey);
                log.info("[消息重试] 消息发送成功，目标主题：{}，消息Key：{}，重试次数：{}", 
                        destination, messageKey, currentRetryCount);
            } else {
                log.warn("[消息重试] 消息发送失败，目标主题：{}，消息Key：{}，重试次数：{}，发送状态：{}", 
                        destination, messageKey, currentRetryCount, sendResult.getSendStatus());
            }
            
            return success;
        } catch (Exception e) {
            log.error("[消息重试] 消息发送异常，目标主题：{}，消息Key：{}，重试次数：{}", 
                    destination, messageKey, currentRetryCount, e);
            return false;
        }
    }
    
    /**
     * 构建重试Key
     */
    private String buildRetryKey(String destination, String messageKey) {
        return StrUtil.format("mq:retry:{}_{}", destination.replace(":", "_"), messageKey);
    }
    
    /**
     * 异步延迟重试发送消息
     */
    public void asyncDelayRetrySendMessage(String destination, Message<?> message, String messageKey, int maxRetryCount, long delaySeconds) {
        // 使用Redis的延时队列功能实现异步延迟重试
        String delayQueueKey = "mq:retry:delay_queue";
        String taskKey = buildRetryKey(destination, messageKey) + ":" + System.currentTimeMillis();
        
        // 存储消息内容
        stringRedisTemplate.opsForValue().set(taskKey, JSON.toJSONString(message), delaySeconds * maxRetryCount, TimeUnit.SECONDS);
        
        // 使用SortedSet实现简单的延时队列
        stringRedisTemplate.opsForZSet().add(
                delayQueueKey, 
                taskKey, 
                System.currentTimeMillis() + delaySeconds * 1000
        );
        
        log.info("[异步延迟重试] 已添加到延迟队列，目标主题：{}，消息Key：{}，延迟时间：{}秒", 
                destination, messageKey, delaySeconds);
    }
}