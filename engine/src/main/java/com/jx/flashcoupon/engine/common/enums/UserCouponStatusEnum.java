

package com.jx.flashcoupon.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户优惠券状态枚举
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-18
 */
@RequiredArgsConstructor
public enum UserCouponStatusEnum {

    /**
     * 未使用
     */
    UNUSED(0),

    /**
     * 锁定
     */
    LOCKING(1),

    /**
     * 已使用
     */
    USED(2),

    /**
     * 已过期
     */
    EXPIRED(3),

    /**
     * 已撤回
     */
    REVOKED(4);

    @Getter
    private final int code;
}
