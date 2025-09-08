

package com.jx.flashcoupon.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建优惠券结算单请求参数实体
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-09-24
 */
@Data
public class CouponCreatePaymentReqDTO {

    /**
     * 用户优惠券ID
     */
    @Schema(description = "用户优惠券ID", required = true)
    private Long couponId;

    /**
     * 订单ID
     */
    @Schema(description = "订单ID", required = true)
    private Long orderId;

    /**
     * 订单金额
     */
    @Schema(description = "订单金额", required = true)
    private BigDecimal orderAmount;

    /**
     * 折扣后金额
     */
    @Schema(description = "折扣后金额", required = true)
    private BigDecimal payableAmount;

    /**
     * 店铺编号
     */
    @Schema(description = "店铺编号", example = "1810714735922956666", required = true)
    private String shopNumber;

    /**
     * 商品集合
     */
    @Schema(description = "商品集合", required = true)
    private List<CouponCreatePaymentGoodsReqDTO> goodsList;
}
