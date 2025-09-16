

package com.jx.flashcoupon.engine.mq.producer;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.engine.common.constant.EngineRockerMQConstant;
import com.jx.flashcoupon.engine.mq.base.BaseSendExtendDTO;
import com.jx.flashcoupon.engine.mq.base.MessageWrapper;
import com.jx.flashcoupon.engine.mq.event.UserCouponRedeemEvent;
import com.jx.flashcoupon.engine.mq.util.MessageRetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 用户兑换优惠券消息生产者
 * 开发时间：2025-09-10
 */
@Slf4j
@Component
public class UserCouponRedeemProducer extends AbstractCommonSendProduceTemplate<UserCouponRedeemEvent> {

    private final ConfigurableEnvironment environment;
    private final MessageRetryUtil messageRetryUtil;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${rocketmq.producer.retry-interval-seconds:60}")
    private long retryIntervalSeconds;

    public UserCouponRedeemProducer(@Autowired RocketMQTemplate rocketMQTemplate, 
                                   @Autowired ConfigurableEnvironment environment,
                                   @Autowired MessageRetryUtil messageRetryUtil) {
        super(rocketMQTemplate);
        this.rocketMQTemplate = rocketMQTemplate;
        this.environment = environment;
        this.messageRetryUtil = messageRetryUtil;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(UserCouponRedeemEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("用户兑换优惠券")
                .keys(UUID.randomUUID().toString())
                .topic(environment.resolvePlaceholders(EngineRockerMQConstant.COUPON_TEMPLATE_REDEEM_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(UserCouponRedeemEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }

    /**
     * 发送消息并添加重试机制
     * @param messageSendEvent 消息事件
     * @return 是否发送成功
     */
    public boolean sendWithRetry(UserCouponRedeemEvent messageSendEvent) {
        BaseSendExtendDTO baseSendExtendParam = buildBaseSendExtendParam(messageSendEvent);
        Message<?> message = buildMessage(messageSendEvent, baseSendExtendParam);
        String destination = baseSendExtendParam.getTopic();
        String messageKey = baseSendExtendParam.getKeys();
        
        try {
            // 尝试直接发送
            SendResult sendResult = rocketMQTemplate.syncSend(destination, message, baseSendExtendParam.getSentTimeout());
            if (ObjectUtil.equal(sendResult.getSendStatus().name(), "SEND_OK")) {
                log.info("[消息发送] 优惠券兑换消息发送成功，消息Key：{}", messageKey);
                return true;
            }
        } catch (Exception e) {
            log.warn("[消息发送] 优惠券兑换消息直接发送失败，准备重试，消息Key：{}", messageKey, e);
        }
        
        // 第一次发送失败，尝试重试
        boolean retrySuccess = messageRetryUtil.retrySendMessage(
                destination, 
                message, 
                messageKey, 
                maxRetryCount, 
                retryIntervalSeconds
        );
        
        // 如果重试也失败，发送到死信队列
        if (!retrySuccess) {
            sendToDeadLetterQueue(messageSendEvent, messageKey);
        }
        
        return retrySuccess;
    }
    
    /**
     * 发送消息到死信队列
     * @param messageSendEvent 消息事件
     * @param messageKey 消息唯一标识
     */
    private void sendToDeadLetterQueue(UserCouponRedeemEvent messageSendEvent, String messageKey) {
        try {
            String dlqTopic = environment.resolvePlaceholders(EngineRockerMQConstant.DLQ_TOPIC_KEY);
            Message<?> dlqMessage = MessageBuilder
                    .withPayload(new MessageWrapper(messageKey, messageSendEvent))
                    .setHeader(MessageConst.PROPERTY_KEYS, messageKey)
                    .setHeader("ORIGIN_TOPIC", environment.resolvePlaceholders(EngineRockerMQConstant.COUPON_TEMPLATE_REDEEM_TOPIC_KEY))
                    .setHeader("RETRY_COUNT", maxRetryCount)
                    .setHeader("SEND_TIME", System.currentTimeMillis())
                    .build();
            
            SendResult sendResult = rocketMQTemplate.syncSend(dlqTopic, dlqMessage);
            log.info("[死信队列] 优惠券兑换消息已发送到死信队列，消息Key：{}, 死信队列发送状态：{}", 
                    messageKey, sendResult.getSendStatus());
        } catch (Exception e) {
            log.error("[死信队列] 优惠券兑换消息发送到死信队列失败，消息Key：{}", messageKey, e);
            // 这里可以添加告警通知，人工介入处理
        }
    }
}

