

package com.jx.flashcoupon.settlement.service.strategy;

import com.nageoffer.onecoupon.settlement.dao.entity.CouponTemplateDO;

import java.math.BigDecimal;

/**
 * CouponCalculationStrategy 是一个用于计算优惠券折扣金额的策略接口。
 * 各种类型的优惠券（如折扣券、满减券等）可以通过实现该接口来定义其具体的折扣计算逻辑。
 *
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-22
 */
public interface CouponCalculationStrategy {

    /**
     * 计算折扣
     *
     * @param template    优惠券模板
     * @param orderAmount 订单金额
     * @return 优惠后金额
     */
    BigDecimal calculateDiscount(CouponTemplateDO template, BigDecimal orderAmount);
}
