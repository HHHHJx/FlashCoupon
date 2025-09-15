

package com.jx.flashcoupon.settlement.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算商品查询优惠券请求参数实体

 * 开发时间：2025-09-24
 */
@Data
public class QueryCouponGoodsReqDTO {

    /**
     * 商品编号
     */
    @Schema(description = "商品编号")
    private String goodsNumber;

    /**
     * 商品价格
     */
    @Schema(description = "商品价格")
    private BigDecimal goodsAmount;
}
