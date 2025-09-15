

package com.jx.flashcoupon.engine.service.handler.remind.impl;

import com.jx.flashcoupon.engine.service.handler.remind.RemindCouponTemplate;
import com.jx.flashcoupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import org.springframework.stereotype.Component;

/**
 * 发送邮件的方式提醒用户抢券
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-07-18
 */
@Component
public class SendEmailRemindCouponTemplate implements RemindCouponTemplate {

    /**
     * 以邮件方式提醒用户抢券
     *
     * @param couponTemplateRemindDTO 提醒所需要的信息
     */
    @Override
    public boolean remind(CouponTemplateRemindDTO couponTemplateRemindDTO) {
        // 空实现
        return true;
    }
}
