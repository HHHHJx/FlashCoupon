

package com.jx.flashcoupon.merchant.admin.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券推送任务执行事件
 * 开发时间：2025-07-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTaskExecuteEvent {

    /**
     * 推送任务id
     */
    private Long couponTaskId;
}
