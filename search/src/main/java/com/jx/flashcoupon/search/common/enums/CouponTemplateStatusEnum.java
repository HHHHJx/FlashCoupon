

package com.jx.flashcoupon.search.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券模板状态枚举
 * <p>
 * 作者：蛋仔
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-31
 */
@RequiredArgsConstructor
public enum CouponTemplateStatusEnum {

    /**
     * 生效中
     */
    EFFECTIVE(0),

    /**
     * 已结束
     */
    EXPIRED(1),

    /**
     * 未删除
     */
    NORMAL(0),

    /**
     * 已删除
     */
    DELETED(1);

    @Getter
    private final Integer code;
}
