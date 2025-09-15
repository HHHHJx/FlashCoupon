

package com.jx.flashcoupon.engine.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 取消预约提醒的布隆过滤器配置
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-07-20
 */
@Configuration
public class RBloomFilterConfiguration {

    /**
     * 优惠券查询缓存穿透布隆过滤器
     */
    @Bean
    public RBloomFilter<String> couponTemplateQueryBloomFilter(RedissonClient redissonClient, @Value("${framework.cache.redis.prefix:}") String cachePrefix) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(cachePrefix + "couponTemplateQueryBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }

    /**
     * 防止取消提醒缓存穿透的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> cancelRemindBloomFilter(RedissonClient redissonClient, @Value("${framework.cache.redis.prefix:}") String cachePrefix) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(cachePrefix + "cancelRemindBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }
}
