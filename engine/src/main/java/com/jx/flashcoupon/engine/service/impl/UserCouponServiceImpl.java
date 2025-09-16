

package com.jx.flashcoupon.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.jx.flashcoupon.engine.common.constant.EngineRedisConstant;
import com.jx.flashcoupon.engine.common.context.UserContext;
import com.jx.flashcoupon.engine.common.enums.RedisStockDecrementErrorEnum;
import com.jx.flashcoupon.engine.common.enums.UserCouponStatusEnum;
import com.jx.flashcoupon.engine.dao.entity.CouponSettlementDO;
import com.jx.flashcoupon.engine.dao.entity.UserCouponDO;
import com.jx.flashcoupon.engine.dao.mapper.CouponSettlementMapper;
import com.jx.flashcoupon.engine.dao.mapper.CouponTemplateMapper;
import com.jx.flashcoupon.engine.dao.mapper.UserCouponMapper;
import com.jx.flashcoupon.engine.dto.req.*;
import com.jx.flashcoupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.jx.flashcoupon.engine.mq.event.UserCouponDelayCloseEvent;
import com.jx.flashcoupon.engine.mq.event.UserCouponRedeemEvent;
import com.jx.flashcoupon.engine.mq.producer.UserCouponDelayCloseProducer;
import com.jx.flashcoupon.engine.mq.producer.UserCouponRedeemProducer;
import com.jx.flashcoupon.engine.service.CouponTemplateService;
import com.jx.flashcoupon.engine.service.UserCouponService;
import com.jx.flashcoupon.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.jx.flashcoupon.framework.exception.ClientException;
import com.jx.flashcoupon.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jx.flashcoupon.engine.common.constant.EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY;

/**
 * 用户优惠券业务逻辑实现层

 * 开发时间：2025-07-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final CouponTemplateService couponTemplateService;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponSettlementMapper couponSettlementMapper;
    private final UserCouponDelayCloseProducer couponDelayCloseProducer;
    private final UserCouponRedeemProducer userCouponRedeemProducer;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    @Value("${flash-coupon.user-coupon-list.save-cache.type:direct}")
    private String userCouponListSaveCacheType;

    private final static String STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH = "lua/stock_decrement_and_save_user_receive.lua";

    // 秒杀/兑换用户优惠券
    @Override
    public void redeemUserCoupon(CouponTemplateRedeemReqDTO requestParam) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 根据流量大小动态选择处理模式
            if (shouldUseAsyncMode()) {
                log.info("[自适应模式] 当前系统负载较高，自动切换到异步模式处理优惠券兑换，用户ID：{}", UserContext.getUserId());
                // 直接调用异步方法处理
                redeemUserCouponByMQ(requestParam);
                return;
            }
            
            // 验证缓存是否存在，保障数据存在并且缓存中存在
            CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplate(BeanUtil.toBean(requestParam, CouponTemplateQueryReqDTO.class));

            // 验证领取的优惠券是否在活动有效时间
            boolean isInTime = DateUtil.isIn(new Date(), couponTemplate.getValidStartTime(), couponTemplate.getValidEndTime());
            if (!isInTime) {
                // 一般来说优惠券领取时间不到的时候，前端不会放开调用请求，可以理解这是用户调用接口在“攻击”
                throw new ClientException("不满足优惠券领取时间");
            }

            // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
            DefaultRedisScript<Long> buildLuaScript = Singleton.get(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH, () -> {
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH)));
                redisScript.setResultType(Long.class);
                return redisScript;
            });

            // 验证用户是否符合优惠券领取条件
            JSONObject receiveRule = JSON.parseObject(couponTemplate.getReceiveRule());
            String limitPerPerson = receiveRule.getString("limitPerPerson");// 优惠券领取限制数量

            // 执行 LUA 脚本进行扣减库存以及增加 Redis 用户领券记录次数
            String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
            String userCouponTemplateLimitCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIMIT_KEY, UserContext.getUserId(), requestParam.getCouponTemplateId());
            Long stockDecrementLuaResult = stringRedisTemplate.execute(
                    buildLuaScript,
                    ListUtil.of(couponTemplateCacheKey, userCouponTemplateLimitCacheKey),
                    String.valueOf(couponTemplate.getValidEndTime().getTime()), limitPerPerson
            );

            // 判断 LUA 脚本执行返回类，如果失败根据类型返回报错提示
            long firstField = StockDecrementReturnCombinedUtil.extractFirstField(stockDecrementLuaResult);
            if (RedisStockDecrementErrorEnum.isFail(firstField)) {
                throw new ServiceException(RedisStockDecrementErrorEnum.fromType(firstField));
            }

            // 通过编程式事务执行优惠券库存自减以及增加用户优惠券领取记录
            long extractSecondField = StockDecrementReturnCombinedUtil.extractSecondField(stockDecrementLuaResult);
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    int decremented = couponTemplateMapper.decrementCouponTemplateStock(Long.parseLong(requestParam.getShopNumber()), Long.parseLong(requestParam.getCouponTemplateId()), 1L);
                    if (!SqlHelper.retBool(decremented)) {
                        throw new ServiceException("优惠券已被领取完啦");
                    }

                    // 添加 Redis 用户领取的优惠券记录列表
                    Date now = new Date();
                    DateTime validEndTime = DateUtil.offsetHour(now, JSON.parseObject(couponTemplate.getConsumeRule()).getInteger("validityPeriod"));
                    UserCouponDO userCouponDO = UserCouponDO.builder()
                            .couponTemplateId(Long.parseLong(requestParam.getCouponTemplateId()))
                            .userId(Long.parseLong(UserContext.getUserId()))
                            .source(requestParam.getSource())
                            .receiveCount(Long.valueOf(extractSecondField).intValue())
                            .status(UserCouponStatusEnum.UNUSED.getCode())
                            .receiveTime(now)
                            .validStartTime(now)
                            .validEndTime(validEndTime)
                            .build();
                    userCouponMapper.insert(userCouponDO);

                    // 保存优惠券缓存集合有两个选项：direct 在流程里直接操作，binlog 通过解析数据库日志后操作
                    if (StrUtil.equals(userCouponListSaveCacheType, "direct")) {
                        // 添加用户领取优惠券模板缓存记录
                        String userCouponListCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId());
                        String userCouponItemCacheKey = StrUtil.builder()
                                .append(requestParam.getCouponTemplateId())
                                .append("_")
                                .append(userCouponDO.getId())
                                .toString();
                        stringRedisTemplate.opsForZSet().add(userCouponListCacheKey, userCouponItemCacheKey, now.getTime());

                        // 由于 Redis 在持久化或主从复制的极端情况下可能会出现数据丢失，而我们对指令丢失几乎无法容忍，因此我们采用经典的写后查询策略来应对这一问题
                        Double scored;
                        try {
                            scored = stringRedisTemplate.opsForZSet().score(userCouponListCacheKey, userCouponItemCacheKey);
                            // scored 为空意味着可能 Redis Cluster 主从同步丢失了数据，比如 Redis 主节点还没有同步到从节点就宕机了，解决方案就是再新增一次
                            if (scored == null) {
                                // 如果这里也新增失败了怎么办？我们大概率做不到绝对的万无一失，只能尽可能增加成功率
                                stringRedisTemplate.opsForZSet().add(userCouponListCacheKey, userCouponItemCacheKey, now.getTime());
                            }
                        } catch (Throwable ex) {
                            log.warn("查询Redis用户优惠券记录为空或抛异常，可能Redis宕机或主从复制数据丢失，基础错误信息：{}", ex.getMessage());
                            // 如果直接抛异常大概率 Redis 宕机了，所以应该写个延时队列向 Redis 重试放入值。为了避免代码复杂性，这里直接写新增，大家知道最优解决方案即可
                            stringRedisTemplate.opsForZSet().add(userCouponListCacheKey, userCouponItemCacheKey, now.getTime());
                        }

                        // 发送延时消息队列，等待优惠券到期后，将优惠券信息从缓存中删除
                        UserCouponDelayCloseEvent userCouponDelayCloseEvent = UserCouponDelayCloseEvent.builder()
                                .couponTemplateId(requestParam.getCouponTemplateId())
                                .userCouponId(String.valueOf(userCouponDO.getId()))
                                .userId(UserContext.getUserId())
                                .delayTime(validEndTime.getTime())
                                .build();
                        
                        // 使用带重试机制和死信队列支持的消息发送方法
                        boolean sendSuccess = couponDelayCloseProducer.sendWithRetry(userCouponDelayCloseEvent);
                        
                        // 发送成功记录信息
                        if (sendSuccess) {
                            log.info("[优惠券关闭] 延时消息发送成功，用户优惠券ID：{}", userCouponDO.getId());
                        } else {
                            // 发送失败但已进入重试机制或死信队列，记录警告日志
                            // 注意：这里不再抛出异常，因为数据库操作已经成功，消息发送失败通过重试机制和死信队列保障
                            log.warn("[优惠券关闭] 延时消息发送失败，已进入重试机制或死信队列，用户优惠券ID：{}", 
                                    userCouponDO.getId());
                            
                            // 可以在这里添加监控告警，通知运维人员关注
                            // sendAlertNotification(userCouponDelayCloseEvent);
                        }
                    }
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    // 优惠券已被领取完业务异常
                    if (ex instanceof ServiceException) {
                        throw (ServiceException) ex;
                    }
                    if (ex instanceof DuplicateKeyException) {
                        log.error("用户重复领取优惠券，用户ID：{}，优惠券模板ID：{}", UserContext.getUserId(), requestParam.getCouponTemplateId());
                        throw new ServiceException("用户重复领取优惠券");
                    }
                    throw new ServiceException("优惠券领取异常，请稍候再试");
                }
            });
            
            // 更新监控指标
            syncSuccessCounter.incrementAndGet();
            log.info("[同步优惠券兑换] 处理成功，用户ID：{}，优惠券模板ID：{}", 
                    UserContext.getUserId(), requestParam.getCouponTemplateId());
        } catch (Exception e) {
            syncFailCounter.incrementAndGet();
            log.error("[同步优惠券兑换] 处理异常，用户ID：{}，优惠券模板ID：{}", 
                    UserContext.getUserId(), requestParam.getCouponTemplateId(), e);
            throw e;
        } finally {
            // 记录响应时间
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            recordResponseTime(syncResponseTimes, responseTime);
        }
    }

    // 计数器指标 - 用于监控
    private static final AtomicInteger syncSuccessCounter = new AtomicInteger(0);
    private static final AtomicInteger syncFailCounter = new AtomicInteger(0);
    private static final AtomicInteger asyncSuccessCounter = new AtomicInteger(0);
    private static final AtomicInteger asyncFailCounter = new AtomicInteger(0);
    private static final ConcurrentLinkedQueue<Long> syncResponseTimes = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> asyncResponseTimes = new ConcurrentLinkedQueue<>();
    private static final int MAX_RESPONSE_TIME_SAMPLES = 100;

    @Override
    public void redeemUserCouponByMQ(CouponTemplateRedeemReqDTO requestParam) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证缓存是否存在，保障数据存在并且缓存中存在
            CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplate(BeanUtil.toBean(requestParam, CouponTemplateQueryReqDTO.class));

            // 验证领取的优惠券是否在活动有效时间
            boolean isInTime = DateUtil.isIn(new Date(), couponTemplate.getValidStartTime(), couponTemplate.getValidEndTime());
            if (!isInTime) {
                // 一般来说优惠券领取时间不到的时候，前端不会放开调用请求，可以理解这是用户调用接口在“攻击”
                throw new ClientException("不满足优惠券领取时间");
            }

            // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
            DefaultRedisScript<Long> buildLuaScript = Singleton.get(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH, () -> {
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH)));
                redisScript.setResultType(Long.class);
                return redisScript;
            });

            // 验证用户是否符合优惠券领取条件
            JSONObject receiveRule = JSON.parseObject(couponTemplate.getReceiveRule());
            String limitPerPerson = receiveRule.getString("limitPerPerson");

            // 执行 LUA 脚本进行扣减库存以及增加 Redis 用户领券记录次数
            String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
            String userCouponTemplateLimitCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIMIT_KEY, UserContext.getUserId(), requestParam.getCouponTemplateId());
            Long stockDecrementLuaResult = stringRedisTemplate.execute(
                    buildLuaScript,
                    ListUtil.of(couponTemplateCacheKey, userCouponTemplateLimitCacheKey),
                    String.valueOf(couponTemplate.getValidEndTime().getTime()), limitPerPerson
            );

            // 判断 LUA 脚本执行返回类，如果失败根据类型返回报错提示
            long firstField = StockDecrementReturnCombinedUtil.extractFirstField(stockDecrementLuaResult);
            if (RedisStockDecrementErrorEnum.isFail(firstField)) {
                throw new ServiceException(RedisStockDecrementErrorEnum.fromType(firstField));
            }

            // 构建幂等性消息ID - 使用请求参数和用户ID构建唯一标识
            String messageId = StrUtil.builder()
                    .append("coupon_redeem_")
                    .append(UserContext.getUserId())
                    .append("_")
                    .append(requestParam.getCouponTemplateId())
                    .append("_")
                    .append(System.currentTimeMillis())
                    .toString();

            UserCouponRedeemEvent userCouponRedeemEvent = UserCouponRedeemEvent.builder()
                    .requestParam(requestParam)
                    .receiveCount((int) StockDecrementReturnCombinedUtil.extractSecondField(stockDecrementLuaResult))
                    .couponTemplate(couponTemplate)
                    .userId(UserContext.getUserId())
                    .build();
            
            // 使用带重试机制和死信队列支持的消息发送方法
            boolean sendSuccess = userCouponRedeemProducer.sendWithRetry(userCouponRedeemEvent);
            
            // 更新监控指标
            if (sendSuccess) {
                asyncSuccessCounter.incrementAndGet();
                log.info("[异步优惠券兑换] 消息发送成功，用户ID：{}，优惠券模板ID：{}", 
                        UserContext.getUserId(), requestParam.getCouponTemplateId());
            } else {
                asyncFailCounter.incrementAndGet();
                log.warn("[异步优惠券兑换] 消息发送失败，已进入重试机制或死信队列，用户ID：{}，优惠券模板ID：{}", 
                        UserContext.getUserId(), requestParam.getCouponTemplateId());
                
                // 可以在这里添加监控告警，通知运维人员关注
                // sendAlertNotification(userCouponRedeemEvent);
            }
        } catch (Exception e) {
            asyncFailCounter.incrementAndGet();
            log.error("[异步优惠券兑换] 处理异常，用户ID：{}，优惠券模板ID：{}", 
                    UserContext.getUserId(), requestParam.getCouponTemplateId(), e);
            throw e;
        } finally {
            // 记录响应时间
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            recordResponseTime(asyncResponseTimes, responseTime);
        }
    }
    
    /**
     * 记录响应时间，并维护样本数量
     */
    private void recordResponseTime(ConcurrentLinkedQueue<Long> queue, long responseTime) {
        queue.add(responseTime);
        if (queue.size() > MAX_RESPONSE_TIME_SAMPLES) {
            queue.poll();
        }
    }
    
    /**
     * 根据流量大小动态选择处理模式
     * 这里使用一个简单的算法：根据最近100次请求的平均响应时间来判断是否切换到异步模式
     * 如果同步模式响应时间超过阈值，则自动切换到异步模式
     */
    private boolean shouldUseAsyncMode() {
        // 简单实现：如果同步模式平均响应时间超过100ms，则使用异步模式
        if (syncResponseTimes.isEmpty()) {
            return false; // 默认使用同步模式
        }
        
        long totalTime = 0;
        for (Long time : syncResponseTimes) {
            totalTime += time;
        }
        long avgTime = totalTime / syncResponseTimes.size();
        
        return avgTime > 100; // 阈值可配置
    }
    
    // 获取监控指标的方法（供外部系统调用）
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("syncSuccessCount", syncSuccessCounter.get());
        metrics.put("syncFailCount", syncFailCounter.get());
        metrics.put("asyncSuccessCount", asyncSuccessCounter.get());
        metrics.put("asyncFailCount", asyncFailCounter.get());
        
        // 计算平均响应时间
        if (!syncResponseTimes.isEmpty()) {
            long totalTime = 0;
            for (Long time : syncResponseTimes) {
                totalTime += time;
            }
            metrics.put("syncAvgResponseTime", totalTime / syncResponseTimes.size());
        }
        
        if (!asyncResponseTimes.isEmpty()) {
            long totalTime = 0;
            for (Long time : asyncResponseTimes) {
                totalTime += time;
            }
            metrics.put("asyncAvgResponseTime", totalTime / asyncResponseTimes.size());
        }
        
        return metrics;
    }

    @Override
    public void createPaymentRecord(CouponCreatePaymentReqDTO requestParam) {
        RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_SETTLEMENT_KEY, requestParam.getCouponId()));
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ClientException("正在创建优惠券结算单，请稍候再试");
        }

        try {
            LambdaQueryWrapper<CouponSettlementDO> queryWrapper = Wrappers.lambdaQuery(CouponSettlementDO.class)
                    .eq(CouponSettlementDO::getCouponId, requestParam.getCouponId())
                    .eq(CouponSettlementDO::getUserId, Long.parseLong(UserContext.getUserId()))
                    .in(CouponSettlementDO::getStatus, 0, 2);

            // 验证优惠券是否正在使用或者已经被使用
            if (couponSettlementMapper.selectOne(queryWrapper) != null) {
                throw new ClientException("请检查优惠券是否已使用");
            }

            UserCouponDO userCouponDO = userCouponMapper.selectOne(Wrappers.lambdaQuery(UserCouponDO.class)
                    .eq(UserCouponDO::getId, requestParam.getCouponId())
                    .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId())));

            // 验证用户优惠券状态和有效性
            if (Objects.isNull(userCouponDO)) {
                throw new ClientException("优惠券不存在");
            }
            if (userCouponDO.getValidEndTime().before(new Date())) {
                throw new ClientException("优惠券已过期");
            }
            if (userCouponDO.getStatus() != 0) {
                throw new ClientException("优惠券使用状态异常");
            }

            // 获取优惠券模板和消费规则
            CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplate(
                    new CouponTemplateQueryReqDTO(requestParam.getShopNumber(), String.valueOf(userCouponDO.getCouponTemplateId())));
            JSONObject consumeRule = JSONObject.parseObject(couponTemplate.getConsumeRule());

            // 计算折扣金额
            BigDecimal discountAmount;

            // 商品专属优惠券
            if (couponTemplate.getTarget().equals(0)) {
                // 获取第一个匹配的商品
                Optional<CouponCreatePaymentGoodsReqDTO> matchedGoods = requestParam.getGoodsList().stream()
                        .filter(each -> Objects.equals(couponTemplate.getGoods(), each.getGoodsNumber()))
                        .findFirst();

                if (matchedGoods.isEmpty()) {
                    throw new ClientException("商品信息与优惠券模板不符");
                }

                // 验证折扣金额
                CouponCreatePaymentGoodsReqDTO paymentGoods = matchedGoods.get();
                BigDecimal maximumDiscountAmount = consumeRule.getBigDecimal("maximumDiscountAmount");
                if (!paymentGoods.getGoodsAmount().subtract(maximumDiscountAmount).equals(paymentGoods.getGoodsPayableAmount())) {
                    throw new ClientException("商品折扣后金额异常");
                }

                discountAmount = maximumDiscountAmount;
            } else { // 店铺专属
                // 检查店铺编号（如果是店铺券）
                if (couponTemplate.getSource() == 0 && !requestParam.getShopNumber().equals(couponTemplate.getShopNumber())) {
                    throw new ClientException("店铺编号不一致");
                }

                BigDecimal termsOfUse = consumeRule.getBigDecimal("termsOfUse");
                if (requestParam.getOrderAmount().compareTo(termsOfUse) < 0) {
                    throw new ClientException("订单金额未满足使用条件");
                }

                BigDecimal maximumDiscountAmount = consumeRule.getBigDecimal("maximumDiscountAmount");

                switch (couponTemplate.getType()) {
                    case 0: // 立减券
                        discountAmount = maximumDiscountAmount;
                        break;
                    case 1: // 满减券
                        discountAmount = maximumDiscountAmount;
                        break;
                    case 2: // 折扣券
                        BigDecimal discountRate = consumeRule.getBigDecimal("discountRate");
                        discountAmount = requestParam.getOrderAmount().multiply(discountRate);
                        if (discountAmount.compareTo(maximumDiscountAmount) >= 0) {
                            discountAmount = maximumDiscountAmount;
                        }
                        break;
                    default:
                        throw new ClientException("无效的优惠券类型");
                }
            }

            // 计算折扣后金额并进行检查
            BigDecimal actualPayableAmount = requestParam.getOrderAmount().subtract(discountAmount);
            if (actualPayableAmount.compareTo(requestParam.getPayableAmount()) != 0) {
                throw new ClientException("折扣后金额不一致");
            }

            // 通过编程式事务减小事务范围
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    // 创建优惠券结算单记录
                    CouponSettlementDO couponSettlementDO = CouponSettlementDO.builder()
                            .orderId(requestParam.getOrderId())
                            .couponId(requestParam.getCouponId())
                            .userId(Long.parseLong(UserContext.getUserId()))
                            .status(0)
                            .build();
                    couponSettlementMapper.insert(couponSettlementDO);

                    // 变更用户优惠券状态
                    LambdaUpdateWrapper<UserCouponDO> userCouponUpdateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                            .eq(UserCouponDO::getId, requestParam.getCouponId())
                            .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                            .eq(UserCouponDO::getStatus, UserCouponStatusEnum.UNUSED.getCode());
                    UserCouponDO updateUserCouponDO = UserCouponDO.builder()
                            .status(UserCouponStatusEnum.LOCKING.getCode())
                            .build();
                    userCouponMapper.update(updateUserCouponDO, userCouponUpdateWrapper);
                } catch (Exception ex) {
                    log.error("创建优惠券结算单失败", ex);
                    status.setRollbackOnly();
                    throw ex;
                }
            });

            // 从用户可用优惠券列表中删除优惠券
            String userCouponItemCacheKey = StrUtil.builder()
                    .append(userCouponDO.getCouponTemplateId())
                    .append("_")
                    .append(userCouponDO.getId())
                    .toString();
            stringRedisTemplate.opsForZSet().remove(String.format(USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), userCouponItemCacheKey);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void processPayment(CouponProcessPaymentReqDTO requestParam) {
        RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_SETTLEMENT_KEY, requestParam.getCouponId()));
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ClientException("正在核销优惠券结算单，请稍候再试");
        }

        // 通过编程式事务减小事务范围
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 变更优惠券结算单状态为已支付
                LambdaUpdateWrapper<CouponSettlementDO> couponSettlementUpdateWrapper = Wrappers.lambdaUpdate(CouponSettlementDO.class)
                        .eq(CouponSettlementDO::getCouponId, requestParam.getCouponId())
                        .eq(CouponSettlementDO::getUserId, Long.parseLong(UserContext.getUserId()))
                        .eq(CouponSettlementDO::getStatus, 0);
                CouponSettlementDO couponSettlementDO = CouponSettlementDO.builder()
                        .status(2)
                        .build();
                int couponSettlementUpdated = couponSettlementMapper.update(couponSettlementDO, couponSettlementUpdateWrapper);
                if (!SqlHelper.retBool(couponSettlementUpdated)) {
                    log.error("核销优惠券结算单异常，请求参数：{}", com.alibaba.fastjson.JSON.toJSONString(requestParam));
                    throw new ServiceException("核销优惠券结算单异常");
                }

                // 变更用户优惠券状态
                LambdaUpdateWrapper<UserCouponDO> userCouponUpdateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                        .eq(UserCouponDO::getId, requestParam.getCouponId())
                        .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                        .eq(UserCouponDO::getStatus, UserCouponStatusEnum.LOCKING.getCode());
                UserCouponDO userCouponDO = UserCouponDO.builder()
                        .status(UserCouponStatusEnum.USED.getCode())
                        .build();
                int userCouponUpdated = userCouponMapper.update(userCouponDO, userCouponUpdateWrapper);
                if (!SqlHelper.retBool(userCouponUpdated)) {
                    log.error("修改用户优惠券记录状态已使用异常，请求参数：{}", com.alibaba.fastjson.JSON.toJSONString(requestParam));
                    throw new ServiceException("修改用户优惠券记录状态异常");
                }
            } catch (Exception ex) {
                log.error("核销优惠券结算单失败", ex);
                status.setRollbackOnly();
                throw ex;
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public void processRefund(CouponProcessRefundReqDTO requestParam) {
        RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_SETTLEMENT_KEY, requestParam.getCouponId()));
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ClientException("正在执行优惠券退款，请稍候再试");
        }

        try {
            // 通过编程式事务减小事务范围
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    // 变更优惠券结算单状态为已退款
                    LambdaUpdateWrapper<CouponSettlementDO> couponSettlementUpdateWrapper = Wrappers.lambdaUpdate(CouponSettlementDO.class)
                            .eq(CouponSettlementDO::getCouponId, requestParam.getCouponId())
                            .eq(CouponSettlementDO::getUserId, Long.parseLong(UserContext.getUserId()))
                            .eq(CouponSettlementDO::getStatus, 2);
                    CouponSettlementDO couponSettlementDO = CouponSettlementDO.builder()
                            .status(3)
                            .build();
                    int couponSettlementUpdated = couponSettlementMapper.update(couponSettlementDO, couponSettlementUpdateWrapper);
                    if (!SqlHelper.retBool(couponSettlementUpdated)) {
                        log.error("优惠券结算单退款异常，请求参数：{}", com.alibaba.fastjson.JSON.toJSONString(requestParam));
                        throw new ServiceException("核销优惠券结算单异常");
                    }

                    // 变更用户优惠券状态
                    LambdaUpdateWrapper<UserCouponDO> userCouponUpdateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                            .eq(UserCouponDO::getId, requestParam.getCouponId())
                            .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                            .eq(UserCouponDO::getStatus, UserCouponStatusEnum.USED.getCode());
                    UserCouponDO userCouponDO = UserCouponDO.builder()
                            .status(UserCouponStatusEnum.UNUSED.getCode())
                            .build();
                    int userCouponUpdated = userCouponMapper.update(userCouponDO, userCouponUpdateWrapper);
                    if (!SqlHelper.retBool(userCouponUpdated)) {
                        log.error("修改用户优惠券记录状态未使用异常，请求参数：{}", com.alibaba.fastjson.JSON.toJSONString(requestParam));
                        throw new ServiceException("修改用户优惠券记录状态异常");
                    }
                } catch (Exception ex) {
                    log.error("执行优惠券结算单退款失败", ex);
                    status.setRollbackOnly();
                    throw ex;
                }
            });

            // 查询出来优惠券再放回缓存
            UserCouponDO userCouponDO = userCouponMapper.selectOne(Wrappers.lambdaQuery(UserCouponDO.class)
                    .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                    .eq(UserCouponDO::getId, requestParam.getCouponId())
            );
            String userCouponItemCacheKey = StrUtil.builder()
                    .append(userCouponDO.getCouponTemplateId())
                    .append("_")
                    .append(userCouponDO.getId())
                    .toString();
            stringRedisTemplate.opsForZSet().add(String.format(USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), userCouponItemCacheKey, userCouponDO.getReceiveTime().getTime());
        } finally {
            lock.unlock();
        }
    }
}
