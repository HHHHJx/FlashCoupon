

package com.jx.flashcoupon.settlement.service.strategy;

import com.jx.flashcoupon.settlement.dao.entity.CouponTemplateDO;
import com.jx.flashcoupon.settlement.dao.entity.ThresholdCouponDO;

import java.math.BigDecimal;

/**
 * 该类用于计算有门槛固定金额优惠券的折扣金额。若满足门槛要求，该优惠券在结算时会直接减去固定的折扣金额
 * 例如，如果优惠券的折扣金额为 50 元，门槛为100 元，满足门槛的订单 使用该优惠券后，订单金额会减少 50 元。
 *
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-08-22
 */
public class ThresholdCalculationStrategy implements CouponCalculationStrategy {

    @Override
    public BigDecimal calculateDiscount(CouponTemplateDO template, BigDecimal orderAmount) {
        ThresholdCouponDO thresholdDiscount = (ThresholdCouponDO) template;
        if (orderAmount.compareTo(BigDecimal.valueOf(thresholdDiscount.getThresholdAmount())) >= 0) {
            return BigDecimal.valueOf(thresholdDiscount.getDiscountAmount());
        }
        return BigDecimal.ZERO;
    }
}
