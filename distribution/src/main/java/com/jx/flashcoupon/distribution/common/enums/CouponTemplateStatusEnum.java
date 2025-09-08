

package com.jx.flashcoupon.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券模板状态枚举
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-14
 */
@RequiredArgsConstructor
public enum CouponTemplateStatusEnum {

    /**
     * 0: 表示优惠券处于生效中的状态。
     */
    ACTIVE(0),

    /**
     * 1: 表示优惠券已经结束，不可再使用。
     */
    ENDED(1);

    @Getter
    private final int status;
}
