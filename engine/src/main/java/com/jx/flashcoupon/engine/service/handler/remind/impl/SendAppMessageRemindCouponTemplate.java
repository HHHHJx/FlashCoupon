

package com.jx.flashcoupon.engine.service.handler.remind.impl;

import com.nageoffer.onecoupon.engine.service.handler.remind.RemindCouponTemplate;
import com.nageoffer.onecoupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import org.springframework.stereotype.Component;

/**
 * 应用 App 弹框方式提醒用户抢券
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-21
 */
@Component
public class SendAppMessageRemindCouponTemplate implements RemindCouponTemplate {

    /**
     * 应用 App 弹框方式提醒用户抢券
     *
     * @param couponTemplateRemindDTO 提醒所需要的信息
     */
    @Override
    public boolean remind(CouponTemplateRemindDTO couponTemplateRemindDTO) {
        // 空实现
        return true;
    }
}
