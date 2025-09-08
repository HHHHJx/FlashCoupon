

package com.jx.flashcoupon.settlement.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 满减券数据库持久层实体
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdCouponDO extends CouponTemplateDO {

    /**
     * 满减门槛金额
     */
    private Integer thresholdAmount;

    /**
     * 优惠金额
     */
    private Integer discountAmount;

    @Builder(builderMethodName = "thresholdCouponBuilder")
    public ThresholdCouponDO(CouponTemplateDO coupon, Integer thresholdAmount, Integer discountAmount) {
        super(coupon.getId(), coupon.getShopNumber(), coupon.getName(), coupon.getSource(), coupon.getTarget(), coupon.getGoods(), coupon.getType(),
                coupon.getValidStartTime(), coupon.getValidEndTime(), coupon.getStock(), coupon.getReceiveRule(), coupon.getConsumeRule(), coupon.getStatus(),
                coupon.getCreateTime(), coupon.getUpdateTime(), coupon.getDelFlag());
        setThresholdAmount(thresholdAmount);
        setDiscountAmount(discountAmount);
    }

    public static ThresholdCouponDOBuilder builder() {
        return new ThresholdCouponDOBuilder();
    }

    public static class ThresholdCouponDOBuilder extends CouponTemplateDO.CouponTemplateDOBuilder {
        private Integer thresholdAmount;
        private Integer discountAmount;

        ThresholdCouponDOBuilder() {
            super();
        }

        public ThresholdCouponDOBuilder thresholdAmount(Integer thresholdAmount) {
            this.thresholdAmount = thresholdAmount;
            return this;
        }

        public ThresholdCouponDOBuilder discountAmount(Integer discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }

        @Override
        public ThresholdCouponDO build() {
            CouponTemplateDO coupon = super.build();
            return new ThresholdCouponDO(coupon, thresholdAmount, discountAmount);
        }
    }
}
