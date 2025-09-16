package com.jx.flashcoupon.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.engine.common.constant.EngineRockerMQConstant;
import com.jx.flashcoupon.engine.mq.base.MessageWrapper;
import com.jx.flashcoupon.engine.mq.event.UserCouponDelayCloseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 用户优惠券延时关闭死信队列消费者
 * 负责处理所有进入死信队列的优惠券关闭消息
 * 开发时间：2025-10-15
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = EngineRockerMQConstant.DLQ_TOPIC_KEY,
        consumerGroup = EngineRockerMQConstant.DLQ_CG_KEY
)
@Slf4j(topic = "UserCouponDelayCloseDlqConsumer")
public class UserCouponDelayCloseDlqConsumer implements RocketMQListener<MessageWrapper<UserCouponDelayCloseEvent>> {

    @Override
    public void onMessage(MessageWrapper<UserCouponDelayCloseEvent> messageWrapper) {
        // 记录死信队列消息，便于人工介入处理
        log.error("[死信队列] 接收到用户优惠券延时关闭消息，消息体：{}", JSON.toJSONString(messageWrapper));
        
        // 这里可以添加告警通知机制，比如发送邮件、短信或企业微信通知给相关负责人
        // sendAlertNotification(messageWrapper);
        
        // 对于死信消息，可以根据业务需要选择不同的处理策略：
        // 1. 记录到数据库，便于后续人工处理
        // 2. 对于可自动重试的消息，可以尝试重新发送到原队列
        // 3. 对于无法自动处理的消息，生成工单通知人工处理
    }
    
    /**
     * 发送告警通知
     * 实际实现中可以对接公司的告警系统
     */
    private void sendAlertNotification(MessageWrapper<UserCouponDelayCloseEvent> messageWrapper) {
        // 这里仅作示例，实际项目中应替换为真实的告警实现
        log.error("[告警] 用户优惠券延时关闭消息进入死信队列，需要人工介入处理！消息ID：{}", messageWrapper.getKeys());
    }
}