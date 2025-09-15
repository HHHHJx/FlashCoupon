

package com.jx.flashcoupon.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 处理优惠券结算单请求参数实体

 * 开发时间：2025-09-24
 */
@Data
public class CouponProcessPaymentReqDTO {

    /**
     * 优惠券ID
     */
    @Schema(description = "优惠券ID", required = true)
    private Long couponId;
}
