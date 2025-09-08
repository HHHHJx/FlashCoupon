

package com.jx.flashcoupon.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis 扣减优惠券库存错误枚举
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-17
 */
@RequiredArgsConstructor
public enum RedisStockDecrementErrorEnum {

    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 库存不足
     */
    STOCK_INSUFFICIENT(1, "优惠券已被领取完啦"),

    /**
     * 用户已经达到领取上限
     */
    LIMIT_REACHED(2, "用户已经达到领取上限");

    @Getter
    private final long code;
    @Getter
    private final String message;

    /**
     * 根据 code 找到对应的枚举实例判断是否成功标识
     *
     * @param code 要查找的编码
     * @return 是否成功标识
     */
    public static boolean isFail(long code) {
        for (RedisStockDecrementErrorEnum status : values()) {
            if (status.code == code) {
                return status != SUCCESS;
            }
        }
        return false;
    }

    /**
     * 根据 type 找到对应的枚举实例
     *
     * @param code 要查找的编码
     * @return 对应的枚举实例
     */
    public static String fromType(long code) {
        for (RedisStockDecrementErrorEnum method : RedisStockDecrementErrorEnum.values()) {
            if (method.getCode() == code) {
                return method.getMessage();
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
