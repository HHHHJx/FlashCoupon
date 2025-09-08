

package com.jx.flashcoupon.engine.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送事件基础扩充属性实体
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class BaseSendExtendDTO {

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    /**
     * 业务标识
     */
    private String keys;

    /**
     * 发送消息超时时间
     */
    private Long sentTimeout;

    /**
     * 具体延迟时间
     */
    private Long delayTime;
}
