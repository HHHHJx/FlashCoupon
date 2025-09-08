

package com.jx.flashcoupon.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 预约提醒方式枚举类，值必须是0，1，2，3......
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-16
 */
@RequiredArgsConstructor
public enum CouponRemindTypeEnum {

    /**
     * App 通知
     */
    APP(0, "App通知"),

    /**
     * 邮件提醒
     */
    EMAIL(1, "邮件提醒");

    @Getter
    private final int type;
    @Getter
    private final String describe;

    public static CouponRemindTypeEnum getByType(Integer type) {
        for (CouponRemindTypeEnum remindEnum : values()) {
            if (remindEnum.getType() == type) {
                return remindEnum;
            }
        }
        return null;
    }

    public static String getDescribeByType(Integer type) {
        for (CouponRemindTypeEnum remindEnum : values()) {
            if (remindEnum.getType() == type) {
                return remindEnum.getDescribe();
            }
        }
        return null;
    }
}
