

package com.jx.flashcoupon.settlement.service.strategy;

import com.nageoffer.onecoupon.settlement.dao.entity.CouponTemplateDO;
import com.nageoffer.onecoupon.settlement.dao.entity.DiscountCouponDO;

import java.math.BigDecimal;

/**
 * DiscountCalculationStrategy 实现了 CouponCalculationStrategy 接口，
 * 负责计算折扣类型优惠券的优惠金额。
 * 例如，如果折扣率为 0.8 且订单金额为 100，折扣后金额将为 80。
 *
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-22
 */
public class DiscountCalculationStrategy implements CouponCalculationStrategy {

    @Override
    public BigDecimal calculateDiscount(CouponTemplateDO template, BigDecimal orderAmount) {
        DiscountCouponDO discountCoupon = (DiscountCouponDO) template;
        return orderAmount.multiply(BigDecimal.valueOf(discountCoupon.getDiscountRate()));
    }
}
