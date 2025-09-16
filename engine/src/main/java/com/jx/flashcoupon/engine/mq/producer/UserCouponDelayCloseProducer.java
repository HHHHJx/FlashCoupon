

package com.jx.flashcoupon.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.engine.common.constant.EngineRockerMQConstant;
import com.jx.flashcoupon.engine.mq.base.BaseSendExtendDTO;
import com.jx.flashcoupon.engine.mq.base.MessageWrapper;
import com.jx.flashcoupon.engine.mq.event.UserCouponDelayCloseEvent;
import com.jx.flashcoupon.engine.mq.util.MessageRetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户优惠券延时关闭生产者
 * 开发时间：2025-07-18
 */
@Slf4j
@Component
public class UserCouponDelayCloseProducer extends AbstractCommonSendProduceTemplate<UserCouponDelayCloseEvent> {

    private final ConfigurableEnvironment environment;
    private final MessageRetryUtil messageRetryUtil;
    private final RocketMQTemplate rocketMQTemplate;

    public UserCouponDelayCloseProducer(@Autowired RocketMQTemplate rocketMQTemplate, 
                                       @Autowired ConfigurableEnvironment environment,
                                       @Autowired MessageRetryUtil messageRetryUtil) {
        super(rocketMQTemplate);
        this.rocketMQTemplate = rocketMQTemplate;
        this.environment = environment;
        this.messageRetryUtil = messageRetryUtil;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(UserCouponDelayCloseEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("延迟关闭用户已领取优惠券")
                .keys(String.valueOf(messageSendEvent.getUserCouponId()))
                .topic(environment.resolvePlaceholders(EngineRockerMQConstant.USER_COUPON_DELAY_CLOSE_TOPIC_KEY))
                .sentTimeout(2000L)
                .delayTime(messageSendEvent.getDelayTime())
                .build();
    }

    @Override
    protected Message<?> buildMessage(UserCouponDelayCloseEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
    
    /**
     * 发送延迟关闭消息（带重试机制）
     * @param event 用户优惠券延迟关闭事件
     * @return 是否发送成功
     */
    public boolean sendWithRetry(UserCouponDelayCloseEvent event) {
        try {
            // 先尝试正常发送
            SendResult sendResult = super.sendMessage(event);
            return "SEND_OK".equals(sendResult.getSendStatus().name());
        } catch (Exception e) {
            log.error("[优惠券关闭] 消息发送失败，尝试重试机制，用户优惠券ID：{}", event.getUserCouponId(), e);
            
            // 构建消息
            BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(event);
            Message<?> message = buildMessage(event, baseSendExtendDTO);
            String destination = baseSendExtendDTO.getTopic();
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destination += ":" + baseSendExtendDTO.getTag();
            }
            
            // 执行重试策略
            boolean retrySuccess = messageRetryUtil.retrySendMessage(
                    destination, 
                    message, 
                    baseSendExtendDTO.getKeys(), 
                    3, // 最大重试3次
                    60 // 重试间隔60秒
            );
            
            if (!retrySuccess) {
                // 重试失败，发送到死信队列
                sendToDeadLetterQueue(event, e);
                return false;
            }
            
            return true;
        }
    }
    
    /**
     * 发送消息到死信队列
     */
    private void sendToDeadLetterQueue(UserCouponDelayCloseEvent event, Exception originalException) {
        try {
            // 构建死信队列消息
            String dlqTopic = environment.resolvePlaceholders(EngineRockerMQConstant.DLQ_TOPIC_KEY);
            String keys = String.valueOf(event.getUserCouponId());
            
            // 包装消息，包含原始异常信息
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("originalEvent", event);
            dlqMessage.put("errorMessage", originalException.getMessage());
            dlqMessage.put("timestamp", System.currentTimeMillis());
            dlqMessage.put("source", "UserCouponDelayCloseProducer");
            
            Message<?> message = MessageBuilder
                    .withPayload(new MessageWrapper(keys, dlqMessage))
                    .setHeader(MessageConst.PROPERTY_KEYS, keys)
                    .build();
            
            // 发送到死信队列
            rocketMQTemplate.syncSend(dlqTopic, message);
            log.error("[优惠券关闭] 消息已发送到死信队列，用户优惠券ID：{}", event.getUserCouponId());
        } catch (Exception ex) {
            log.error("[优惠券关闭] 发送死信队列失败，用户优惠券ID：{}", event.getUserCouponId(), ex);
            // 记录到数据库或其他持久化存储，确保消息不丢失
            // saveToFailedMessageLog(event, originalException);
        }
    }
}
