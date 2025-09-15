

package com.jx.flashcoupon.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jx.flashcoupon.merchant.admin.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板数据库持久层

 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    /**
     * 增加或减少优惠券模板发行量（支持正负增量，原子更新，避免负库存）
     *
     * @param shopNumber       店铺编号
     * @param couponTemplateId 优惠券模板 ID
     * @param number           变更发行数量（正为增加，负为减少）
     */
    @Update("UPDATE t_coupon_template SET stock = stock + #{number} " +
            "WHERE shop_number = #{shopNumber} " +
            "AND id = #{couponTemplateId} " +
            "AND stock + #{number} >= 0")
    int increaseNumberCouponTemplate(@Param("shopNumber") Long shopNumber, @Param("couponTemplateId") String couponTemplateId, @Param("number") Integer number);
}
