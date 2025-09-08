package com.jx.flashcoupon.merchantadmin.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.jx.flashcoupon.merchantadmin.common.constant.MerchantAdminRocketMQConstant;
import com.jx.flashcoupon.merchantadmin.mq.base.BaseSendExtendDTO;
import com.jx.flashcoupon.merchantadmin.mq.base.MessageWrapper;
import com.jx.flashcoupon.merchantadmin.mq.event.CouponTemplateDelayEvent;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 优惠券模板关闭定时执行生产者
 */
@Component
public class CouponTemplateDelayExecuteStatusProducer extends AbstractCommonSendProduceTemplate<CouponTemplateDelayEvent> {

    private final ConfigurableEnvironment environment;

    public CouponTemplateDelayExecuteStatusProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTemplateDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券模板关闭定时执行")
                .keys(String.valueOf(messageSendEvent.getCouponTemplateId()))
                .topic(environment.resolvePlaceholders(MerchantAdminRocketMQConstant.TEMPLATE_DELAY_TOPIC_KEY))
                .delayTime(messageSendEvent.getDelayTime())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDelayEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
