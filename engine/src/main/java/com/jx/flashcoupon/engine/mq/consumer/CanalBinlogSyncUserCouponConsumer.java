

package com.jx.flashcoupon.engine.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.engine.common.constant.EngineRedisConstant;
import com.jx.flashcoupon.engine.common.constant.EngineRockerMQConstant;
import com.jx.flashcoupon.engine.common.enums.UserCouponStatusEnum;
import com.jx.flashcoupon.engine.mq.event.CanalBinlogEvent;
import com.jx.flashcoupon.engine.mq.event.UserCouponDelayCloseEvent;
import com.jx.flashcoupon.engine.mq.producer.UserCouponDelayCloseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 通过 Canal 监听用户优惠券表 Binlog 投递消息队列消费，保证缓存与数据库一致性
 * 支持 INSERT/UPDATE/DELETE 三种操作类型的缓存同步
 * 
 * 开发时间：2025-07-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = EngineRockerMQConstant.USER_COUPON_BINLOG_SYNC_TOPIC_KEY,
        consumerGroup = EngineRockerMQConstant.USER_COUPON_BINLOG_SYNC_CG_KEY,
        consumeThreadMax = 10, // 增加消费线程数，提高并发处理能力
        maxReconsumeTimes = 3 // 设置最大重试次数为3次
)
public class CanalBinlogSyncUserCouponConsumer implements RocketMQListener<CanalBinlogEvent> {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserCouponDelayCloseProducer couponDelayCloseProducer;

    @Value("${flash-coupon.user-coupon-list.save-cache.type:direct}")
    private String userCouponListSaveCacheType;

    @Override
    public void onMessage(CanalBinlogEvent canalBinlogEvent) {
        if (ObjectUtil.notEqual(userCouponListSaveCacheType, "binlog")) {
            log.debug("当前缓存保存类型不是binlog，跳过处理");
            return;
        }
        
        try {
            // 记录binlog事件接收情况
            log.debug("接收到用户优惠券Binlog事件，类型：{}，数据库：{}，表名：{}", 
                    canalBinlogEvent.getType(), canalBinlogEvent.getDatabase(), canalBinlogEvent.getTable());
            
            Map<String, Object> first = CollUtil.getFirst(canalBinlogEvent.getData());
            if (first == null) {
                log.warn("Binlog事件数据为空，跳过处理，事件：{}", JSON.toJSONString(canalBinlogEvent));
                return;
            }
            
            String userId = first.get("user_id").toString();
            String couponTemplateId = first.get("coupon_template_id").toString();
            String userCouponId = first.get("id").toString();
            
            // 根据事件类型执行不同的缓存同步逻辑
            if (ObjectUtil.equal(canalBinlogEvent.getType(), "INSERT")) {
                handleInsertEvent(userId, couponTemplateId, userCouponId, first);
            } else if (ObjectUtil.equal(canalBinlogEvent.getType(), "UPDATE")) {
                handleUpdateEvent(userId, couponTemplateId, userCouponId, first, canalBinlogEvent);
            } else if (ObjectUtil.equal(canalBinlogEvent.getType(), "DELETE")) {
                handleDeleteEvent(userId, couponTemplateId, userCouponId);
            }
        } catch (Exception e) {
            // 捕获所有异常，避免影响消息队列的正常消费
            log.error("处理用户优惠券Binlog事件异常，事件：{}", JSON.toJSONString(canalBinlogEvent), e);
            // 可以在这里添加监控告警，通知运维人员关注
            // sendAlertNotification(canalBinlogEvent, e);
            // 抛出异常触发重试机制
            throw new RuntimeException("处理Binlog事件失败，触发重试机制", e);
        }
    }
    
    /**
     * 处理用户优惠券创建事件
     */
    private void handleInsertEvent(String userId, String couponTemplateId, String userCouponId, Map<String, Object> data) {
        log.info("处理用户优惠券创建事件，用户ID：{}，优惠券模板ID：{}，用户优惠券ID：{}", userId, couponTemplateId, userCouponId);
        
        // 添加用户领取优惠券模板缓存记录
        String userCouponListCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, userId);
        String userCouponItemCacheKey = StrUtil.builder()
                .append(couponTemplateId)
                .append("_")
                .append(userCouponId)
                .toString();
        
        Date receiveTime = DateUtil.parse(data.get("receive_time").toString());
        boolean addResult = false;
        
        // 重试机制：最多尝试3次
        int retryCount = 0;
        while (retryCount < 3 && !addResult) {
            try {
                stringRedisTemplate.opsForZSet().add(userCouponListCacheKey, userCouponItemCacheKey, receiveTime.getTime());
                
                // 写后查询策略，验证数据是否写入成功
                Double scored = stringRedisTemplate.opsForZSet().score(userCouponListCacheKey, userCouponItemCacheKey);
                if (scored != null) {
                    addResult = true;
                    log.debug("用户优惠券缓存添加成功，用户ID：{}，缓存key：{}", userId, userCouponItemCacheKey);
                } else {
                    log.warn("用户优惠券缓存添加后验证失败，可能Redis主从复制数据丢失，重试第{}次，用户ID：{}", 
                            retryCount + 1, userId);
                    retryCount++;
                    // 指数退避等待
                    Thread.sleep(100 * (long)Math.pow(2, retryCount));
                }
            } catch (Exception e) {
                log.warn("用户优惠券缓存添加异常，重试第{}次，用户ID：{}", retryCount + 1, userId, e);
                retryCount++;
                // 指数退避等待
                try {
                    Thread.sleep(100 * (long)Math.pow(2, retryCount));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (!addResult) {
            log.error("用户优惠券缓存添加失败，已达到最大重试次数，用户ID：{}", userId);
            // 添加到失败队列，等待人工处理或定时任务重试
            // addToFailQueue("INSERT", userId, couponTemplateId, userCouponId);
        }
        
        // 发送延时消息队列，等待优惠券到期后，将优惠券信息从缓存中删除
        UserCouponDelayCloseEvent userCouponDelayCloseEvent = UserCouponDelayCloseEvent.builder()
                .couponTemplateId(couponTemplateId)
                .userCouponId(userCouponId)
                .userId(userId)
                .delayTime(DateUtil.parse(data.get("valid_end_time").toString()).getTime())
                .build();
        
        // 使用带重试机制的消息发送方法
        boolean sendSuccess = couponDelayCloseProducer.sendWithRetry(userCouponDelayCloseEvent);
        
        if (sendSuccess) {
            log.info("[优惠券关闭] 延时消息发送成功，用户优惠券ID：{}", userCouponId);
        } else {
            log.warn("[优惠券关闭] 延时消息发送失败，已进入重试机制或死信队列，用户优惠券ID：{}", userCouponId);
        }
    }
    
    /**
     * 处理用户优惠券更新事件
     */
    private void handleUpdateEvent(String userId, String couponTemplateId, String userCouponId, Map<String, Object> data, CanalBinlogEvent canalBinlogEvent) {
        log.info("处理用户优惠券更新事件，用户ID：{}，优惠券模板ID：{}，用户优惠券ID：{}", userId, couponTemplateId, userCouponId);
        
        String userCouponListCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, userId);
        String userCouponItemCacheKey = StrUtil.builder()
                .append(couponTemplateId)
                .append("_")
                .append(userCouponId)
                .toString();
        
        // 检查状态字段是否有更新
        Object statusObj = data.get("status");
        if (statusObj != null) {
            Integer status = Integer.parseInt(statusObj.toString());
            
            // 如果优惠券状态变为已使用或已过期，从缓存中删除
            if (UserCouponStatusEnum.USED.getCode() == status ||
                UserCouponStatusEnum.EXPIRED.getCode() == status) {
                
                boolean removeResult = false;
                int retryCount = 0;
                
                // 重试机制：最多尝试3次
                while (retryCount < 3 && !removeResult) {
                    try {
                        stringRedisTemplate.opsForZSet().remove(userCouponListCacheKey, userCouponItemCacheKey);
                        
                        // 验证删除是否成功
                        Double scored = stringRedisTemplate.opsForZSet().score(userCouponListCacheKey, userCouponItemCacheKey);
                        if (scored == null) {
                            removeResult = true;
                            log.debug("用户优惠券缓存删除成功，用户ID：{}，缓存key：{}", userId, userCouponItemCacheKey);
                        } else {
                            log.warn("用户优惠券缓存删除后验证失败，重试第{}次，用户ID：{}", 
                                    retryCount + 1, userId);
                            retryCount++;
                            // 指数退避等待
                            Thread.sleep(100 * (long)Math.pow(2, retryCount));
                        }
                    } catch (Exception e) {
                        log.warn("用户优惠券缓存删除异常，重试第{}次，用户ID：{}", retryCount + 1, userId, e);
                        retryCount++;
                        // 指数退避等待
                        try {
                            Thread.sleep(100 * (long)Math.pow(2, retryCount));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                
                if (!removeResult) {
                    log.error("用户优惠券缓存删除失败，已达到最大重试次数，用户ID：{}", userId);
                    // 添加到失败队列，等待人工处理或定时任务重试
                    // addToFailQueue("UPDATE", userId, couponTemplateId, userCouponId);
                }
                
                // 取消对应的延时消息（如果系统支持的话）
                // cancelDelayMessage(userCouponDelayCloseEvent);
            }
        }
        
        // 如果有效期有更新，更新延时消息
        Object validEndTimeObj = data.get("valid_end_time");
        if (validEndTimeObj != null) {
            // 检查是否有旧的有效期值（canal可能会提供before数据）
            Map<String, Object> oldData = CollUtil.getFirst(canalBinlogEvent.getOld());
            if (oldData != null && !ObjectUtil.equal(oldData.get("valid_end_time"), validEndTimeObj)) {
                // 有效期发生了变化，更新延时消息
                UserCouponDelayCloseEvent userCouponDelayCloseEvent = UserCouponDelayCloseEvent.builder()
                        .couponTemplateId(couponTemplateId)
                        .userCouponId(userCouponId)
                        .userId(userId)
                        .delayTime(DateUtil.parse(validEndTimeObj.toString()).getTime())
                        .build();
                
                boolean sendSuccess = couponDelayCloseProducer.sendWithRetry(userCouponDelayCloseEvent);
                
                if (sendSuccess) {
                    log.info("[优惠券关闭] 延时消息更新成功，用户优惠券ID：{}", userCouponId);
                } else {
                    log.warn("[优惠券关闭] 延时消息更新失败，已进入重试机制或死信队列，用户优惠券ID：{}", userCouponId);
                }
            }
        }
    }
    
    /**
     * 处理用户优惠券删除事件
     */
    private void handleDeleteEvent(String userId, String couponTemplateId, String userCouponId) {
        log.info("处理用户优惠券删除事件，用户ID：{}，优惠券模板ID：{}，用户优惠券ID：{}", userId, couponTemplateId, userCouponId);
        
        String userCouponListCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, userId);
        String userCouponItemCacheKey = StrUtil.builder()
                .append(couponTemplateId)
                .append("_")
                .append(userCouponId)
                .toString();
        
        boolean removeResult = false;
        int retryCount = 0;
        
        // 重试机制：最多尝试3次
        while (retryCount < 3 && !removeResult) {
            try {
                stringRedisTemplate.opsForZSet().remove(userCouponListCacheKey, userCouponItemCacheKey);
                
                // 验证删除是否成功
                Double scored = stringRedisTemplate.opsForZSet().score(userCouponListCacheKey, userCouponItemCacheKey);
                if (scored == null) {
                    removeResult = true;
                    log.debug("用户优惠券缓存删除成功，用户ID：{}，缓存key：{}", userId, userCouponItemCacheKey);
                } else {
                    log.warn("用户优惠券缓存删除后验证失败，重试第{}次，用户ID：{}", 
                            retryCount + 1, userId);
                    retryCount++;
                    // 指数退避等待
                    Thread.sleep(100 * (long)Math.pow(2, retryCount));
                }
            } catch (Exception e) {
                log.warn("用户优惠券缓存删除异常，重试第{}次，用户ID：{}", retryCount + 1, userId, e);
                retryCount++;
                // 指数退避等待
                try {
                    Thread.sleep(100 * (long)Math.pow(2, retryCount));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (!removeResult) {
            log.error("用户优惠券缓存删除失败，已达到最大重试次数，用户ID：{}", userId);
            // 添加到失败队列，等待人工处理或定时任务重试
            // addToFailQueue("DELETE", userId, couponTemplateId, userCouponId);
        }
        
        // 取消对应的延时消息（如果系统支持的话）
        // cancelDelayMessage(userCouponDelayCloseEvent);
    }
    

}
