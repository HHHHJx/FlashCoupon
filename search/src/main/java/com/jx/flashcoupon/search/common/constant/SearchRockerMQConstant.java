

package com.jx.flashcoupon.search.common.constant;

/**
 * 优惠券搜索层服务 RocketMQ 常量类
 * <p>
 * 作者：蛋仔
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-30
 */
public final class SearchRockerMQConstant {

    /**
     * Canal 监听优惠券模板表 Binlog Topic Key
     */
    public static final String TEMPLATE_COUPON_BINLOG_SYNC_TOPIC_KEY = "one-coupon_canal_search-service_es-sync_topic${unique-name:}";

    /**
     * Canal 监听优惠券模板表 Binlog 消费者组 Key
     */
    public static final String TEMPLATE_COUPON_BINLOG_SYNC_CG_KEY = "one-coupon_canal_search-service_es-sync_cg${unique-name:}";
}
