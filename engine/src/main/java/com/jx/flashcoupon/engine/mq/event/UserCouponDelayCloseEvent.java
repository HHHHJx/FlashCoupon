

package com.jx.flashcoupon.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户优惠券延时关闭事件
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-18
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
