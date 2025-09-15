

package com.jx.flashcoupon.engine.mq.event;

import com.jx.flashcoupon.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.jx.flashcoupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户兑换优惠券事件

 * 开发时间：2025-09-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponRedeemEvent {

    /**
     * Web 请求参数
     */
    private CouponTemplateRedeemReqDTO requestParam;

    /**
     * 领取次数
     */
    private Integer receiveCount;

    /**
     * 优惠券模板
     */
    private CouponTemplateQueryRespDTO couponTemplate;

    /**
     * 用户 ID
     */
    private String userId;
}
