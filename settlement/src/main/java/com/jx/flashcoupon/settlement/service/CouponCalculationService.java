

package com.jx.flashcoupon.settlement.service;

import com.nageoffer.onecoupon.settlement.dao.entity.CouponTemplateDO;
import com.nageoffer.onecoupon.settlement.service.strategy.CouponCalculationStrategy;
import com.nageoffer.onecoupon.settlement.toolkit.CouponFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * CouponCalculationService 是一个用于计算优惠金额的服务类。
 * 它根据不同的优惠券类型，动态选择相应的计算策略，并返回计算后的优惠金额。
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-29
 */
@Service
public class CouponCalculationService {

    /**
     * 计算优惠金额。
     * 根据传入的优惠券实例和订单金额，选择相应的计算策略，返回最终的优惠金额。
     *
     * @param coupon      具体的优惠券实例
     * @param orderAmount 订单金额
     * @return 计算出的优惠金额
     */
    public BigDecimal calculateDiscount(CouponTemplateDO coupon, BigDecimal orderAmount) {
        CouponCalculationStrategy strategy = CouponFactory.getCouponCalculationStrategy(coupon);
        return strategy.calculateDiscount(coupon, orderAmount);
    }
}
