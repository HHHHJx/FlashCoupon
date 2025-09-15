

package com.jx.flashcoupon.engine.mq.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.engine.common.constant.EngineRockerMQConstant;
import com.jx.flashcoupon.engine.mq.base.MessageWrapper;
import com.jx.flashcoupon.engine.mq.event.CouponTemplateRemindDelayEvent;
import com.jx.flashcoupon.engine.service.handler.remind.CouponTemplateRemindExecutor;
import com.jx.flashcoupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 提醒抢券消费者
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-07-21
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = EngineRockerMQConstant.COUPON_TEMPLATE_REMIND_TOPIC_KEY,
        consumerGroup = EngineRockerMQConstant.COUPON_TEMPLATE_REMIND_CG_KEY
)
@Slf4j(topic = "CouponTemplateRemindDelayConsumer")
public class CouponTemplateRemindDelayConsumer implements RocketMQListener<MessageWrapper<CouponTemplateRemindDelayEvent>> {

    private final CouponTemplateRemindExecutor couponTemplateRemindExecutor;

    @Override
    public void onMessage(MessageWrapper<CouponTemplateRemindDelayEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 提醒用户抢券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        CouponTemplateRemindDelayEvent event = messageWrapper.getMessage();
        CouponTemplateRemindDTO couponTemplateRemindDTO = BeanUtil.toBean(event, CouponTemplateRemindDTO.class);

        // 根据不同策略向用户发送消息提醒
        couponTemplateRemindExecutor.executeRemindCouponTemplate(couponTemplateRemindDTO);
    }
}
