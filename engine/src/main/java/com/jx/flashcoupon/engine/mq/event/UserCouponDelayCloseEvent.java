

package com.jx.flashcoupon.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户优惠券延时关闭事件
 * 开发时间：2025-07-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponDelayCloseEvent {

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户优惠券id
     */
    private String userCouponId;

    /**
     * 优惠券模板id
     */
    private String couponTemplateId;

    /**
     * 具体延迟时间
     */
    private Long delayTime;
}
