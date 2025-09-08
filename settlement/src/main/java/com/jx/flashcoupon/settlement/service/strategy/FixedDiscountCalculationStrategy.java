

package com.jx.flashcoupon.settlement.service.strategy;

import com.nageoffer.onecoupon.settlement.dao.entity.CouponTemplateDO;
import com.nageoffer.onecoupon.settlement.dao.entity.FixedDiscountCouponDO;

import java.math.BigDecimal;

/**
 * 该类用于计算固定金额优惠券的折扣金额。固定金额优惠券在结算时会直接减去固定的折扣金额
 * 例如，如果优惠券的折扣金额为 50 元，使用该优惠券后，订单金额都会减少 50 元。
 *
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-22
 */
public class FixedDiscountCalculationStrategy implements CouponCalculationStrategy {

    @Override
    public BigDecimal calculateDiscount(CouponTemplateDO template, BigDecimal orderAmount) {
        FixedDiscountCouponDO fixedDiscount = (FixedDiscountCouponDO) template;
        return BigDecimal.valueOf(fixedDiscount.getDiscountAmount());
    }
}
