

package com.jx.flashcoupon.settlement.toolkit;

import com.nageoffer.onecoupon.settlement.common.enums.DiscountTypeEnum;
import com.nageoffer.onecoupon.settlement.dao.entity.CouponTemplateDO;
import com.nageoffer.onecoupon.settlement.dao.entity.DiscountCouponDO;
import com.nageoffer.onecoupon.settlement.dao.entity.FixedDiscountCouponDO;
import com.nageoffer.onecoupon.settlement.dao.entity.ThresholdCouponDO;
import com.nageoffer.onecoupon.settlement.service.strategy.CouponCalculationStrategy;
import com.nageoffer.onecoupon.settlement.service.strategy.DiscountCalculationStrategy;
import com.nageoffer.onecoupon.settlement.service.strategy.FixedDiscountCalculationStrategy;
import com.nageoffer.onecoupon.settlement.service.strategy.ThresholdCalculationStrategy;

import java.util.Map;

/**
 * 优惠券工厂类，用于创建不同类型的优惠券实例
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-24
 */
public class CouponFactory {

    /**
     * 创建优惠券对象，根据传入的 CouponTemplateDO 对象和附加参数生成具体的优惠券实例。
     *
     * @param coupon           基础优惠券模板对象
     * @param additionalParams 附加参数，包含优惠券类型所需的额外信息
     * @return 具体的优惠券实例
     */
    public static CouponTemplateDO createCoupon(CouponTemplateDO coupon, Map<String, Object> additionalParams) {
        // 检查优惠券类型是否有效
        if (coupon.getType() == null || coupon.getType() >= DiscountTypeEnum.values().length || coupon.getType() < 0) {
            throw new IllegalArgumentException("Invalid coupon type");
        }

        // 根据优惠券类型创建具体的优惠券实例
        switch (DiscountTypeEnum.values()[coupon.getType()]) {
            case FIXED_DISCOUNT:
                // 固定折扣类型优惠券
                Integer fixedDiscountAmount = (Integer) additionalParams.get("discountAmount");
                return new FixedDiscountCouponDO(coupon, fixedDiscountAmount);

            case THRESHOLD_DISCOUNT:
                // 阈值折扣类型优惠券
                Integer thresholdAmount = (Integer) additionalParams.get("thresholdAmount");
                Integer thresholdDiscountAmount = (Integer) additionalParams.get("discountAmount");
                return new ThresholdCouponDO(coupon, thresholdAmount, thresholdDiscountAmount);

            case DISCOUNT_COUPON:
                // 折扣券类型优惠券
                Double discountRate = (Double) additionalParams.get("discountRate");
                return new DiscountCouponDO(coupon, discountRate);

            default:
                // 如果类型无效，抛出异常
                throw new IllegalArgumentException("Invalid coupon type");
        }
    }

    /**
     * 获取优惠券计算策略。
     *
     * @param coupon 基础优惠券模板对象
     * @return 对应的优惠券计算策略
     */
    public static CouponCalculationStrategy getCouponCalculationStrategy(CouponTemplateDO coupon) {
        switch (DiscountTypeEnum.values()[coupon.getType()]) {
            case FIXED_DISCOUNT:
                return new FixedDiscountCalculationStrategy();
            case THRESHOLD_DISCOUNT:
                return new ThresholdCalculationStrategy();
            case DISCOUNT_COUPON:
                return new DiscountCalculationStrategy();
            default:
                throw new IllegalArgumentException("Invalid coupon type");
        }
    }
}