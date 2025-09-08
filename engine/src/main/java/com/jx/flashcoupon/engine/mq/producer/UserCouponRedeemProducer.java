

package com.jx.flashcoupon.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.nageoffer.onecoupon.engine.common.constant.EngineRockerMQConstant;
import com.nageoffer.onecoupon.engine.mq.base.BaseSendExtendDTO;
import com.nageoffer.onecoupon.engine.mq.base.MessageWrapper;
import com.nageoffer.onecoupon.engine.mq.event.UserCouponRedeemEvent;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 用户兑换优惠券消息生产者
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-09-10
 */
@Component
public class UserCouponRedeemProducer extends AbstractCommonSendProduceTemplate<UserCouponRedeemEvent> {

    private final ConfigurableEnvironment environment;

    public UserCouponRedeemProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
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
}

