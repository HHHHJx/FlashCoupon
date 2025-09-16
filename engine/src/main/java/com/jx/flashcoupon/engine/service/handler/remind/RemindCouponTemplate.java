

package com.jx.flashcoupon.engine.service.handler.remind;

import com.jx.flashcoupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

/**
 * 优惠券抢券提醒接口

 * 开发时间：2025-07-18
 */
public interface RemindCouponTemplate {

    /**
     * 提醒用户抢券
     *
     * @param remindCouponTemplateDTO 提醒所需要的信息
     */
    boolean remind(CouponTemplateRemindDTO remindCouponTemplateDTO);
}
