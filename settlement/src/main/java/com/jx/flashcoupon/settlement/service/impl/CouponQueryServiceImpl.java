

package com.jx.flashcoupon.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jx.flashcoupon.framework.config.RedisDistributedProperties;
import com.jx.flashcoupon.framework.exception.ClientException;
import com.jx.flashcoupon.settlement.common.context.UserContext;
import com.jx.flashcoupon.settlement.dto.req.QueryCouponGoodsReqDTO;
import com.jx.flashcoupon.settlement.dto.req.QueryCouponsReqDTO;
import com.jx.flashcoupon.settlement.dto.resp.CouponTemplateQueryRespDTO;
import com.jx.flashcoupon.settlement.dto.resp.QueryCouponsDetailRespDTO;
import com.jx.flashcoupon.settlement.dto.resp.QueryCouponsRespDTO;
import com.jx.flashcoupon.settlement.service.CouponQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jx.flashcoupon.settlement.common.constant.EngineRedisConstant.COUPON_TEMPLATE_KEY;
import static com.jx.flashcoupon.settlement.common.constant.EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY;

/**
 * 查询用户可用优惠券列表接口
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-07-25
 */
@Service
@RequiredArgsConstructor
public class CouponQueryServiceImpl implements CouponQueryService {

    private final RedisDistributedProperties redisDistributedProperties;
    private final StringRedisTemplate stringRedisTemplate;

    // 在我们本次的业务场景中，属于是 CPU 密集型任务，设置 CPU 的核心数即可
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            9999,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public QueryCouponsRespDTO listQueryUserCoupons(QueryCouponsReqDTO requestParam) {
        // Step 1: 获取 Redis 中的用户优惠券列表
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(
                String.format(USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), 0, -1);

        if (rangeUserCoupons == null || rangeUserCoupons.isEmpty()) {
            return QueryCouponsRespDTO.builder()
                    .availableCouponList(new ArrayList<>())
                    .notAvailableCouponList(new ArrayList<>())
                    .build();
        }

        // 构建 Redis Key 列表
        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(each -> redisDistributedProperties.getPrefix() + String.format(COUPON_TEMPLATE_KEY, each))
                .toList();

        // 同步获取 Redis 数据并进行解析、转换和分区
        List<Object> rawCouponDataList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        // 解析 Redis 数据，并按 `goods` 字段进行分区处理
        Map<Boolean, List<CouponTemplateQueryRespDTO>> partitioned = JSON.parseArray(JSON.toJSONString(rawCouponDataList), CouponTemplateQueryRespDTO.class)
                .stream()
                .collect(Collectors.partitioningBy(coupon -> StrUtil.isEmpty(coupon.getGoods())));

        // 拆分后的两个列表
        List<CouponTemplateQueryRespDTO> goodsEmptyList = partitioned.get(true); // goods 为空的列表
        List<CouponTemplateQueryRespDTO> goodsNotEmptyList = partitioned.get(false); // goods 不为空的列表

        // 针对当前订单可用/不可用的优惠券列表
        List<QueryCouponsDetailRespDTO> availableCouponList = Collections.synchronizedList(new ArrayList<>());
        List<QueryCouponsDetailRespDTO> notAvailableCouponList = Collections.synchronizedList(new ArrayList<>());

        // Step 2: 并行处理 goodsEmptyList 和 goodsNotEmptyList 中的每个元素
        CompletableFuture<Void> emptyGoodsTasks = CompletableFuture.allOf(
                goodsEmptyList.stream()
                        .map(each -> CompletableFuture.runAsync(() -> {
                            QueryCouponsDetailRespDTO resultCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
                            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
                            handleCouponLogic(resultCouponDetail, jsonObject, requestParam.getOrderAmount(), availableCouponList, notAvailableCouponList);
                        }, executorService))
                        .toArray(CompletableFuture[]::new)
        );

        Map<String, QueryCouponGoodsReqDTO> goodsRequestMap = requestParam.getGoodsList().stream()
                .collect(Collectors.toMap(QueryCouponGoodsReqDTO::getGoodsNumber, Function.identity()));
        CompletableFuture<Void> notEmptyGoodsTasks = CompletableFuture.allOf(
                goodsNotEmptyList.stream()
                        .map(each -> CompletableFuture.runAsync(() -> {
                            QueryCouponsDetailRespDTO resultCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
                            QueryCouponGoodsReqDTO couponGoods = goodsRequestMap.get(each.getGoods());
                            if (couponGoods == null) {
                                notAvailableCouponList.add(resultCouponDetail);
                            } else {
                                JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
                                handleCouponLogic(resultCouponDetail, jsonObject, couponGoods.getGoodsAmount(), availableCouponList, notAvailableCouponList);
                            }
                        }, executorService))
                        .toArray(CompletableFuture[]::new)
        );

        // Step 3: 等待两个异步任务集合完成
        CompletableFuture.allOf(emptyGoodsTasks, notEmptyGoodsTasks)
                .thenRun(() -> {
                    // 与业内标准一致，按最终优惠力度从大到小排序
                    availableCouponList.sort((c1, c2) -> c2.getCouponAmount().compareTo(c1.getCouponAmount()));
                })
                .join();

        // 构建最终结果并返回
        return QueryCouponsRespDTO.builder()
                .availableCouponList(availableCouponList)
                .notAvailableCouponList(notAvailableCouponList)
                .build();
    }

    // 优惠券判断逻辑，根据条件判断放入可用或不可用列表
    private void handleCouponLogic(QueryCouponsDetailRespDTO resultCouponDetail, JSONObject jsonObject, BigDecimal amount,
                                   List<QueryCouponsDetailRespDTO> availableCouponList, List<QueryCouponsDetailRespDTO> notAvailableCouponList) {
        BigDecimal termsOfUse = jsonObject.getBigDecimal("termsOfUse");
        BigDecimal maximumDiscountAmount = jsonObject.getBigDecimal("maximumDiscountAmount");

        switch (resultCouponDetail.getType()) {
            case 0: // 立减券
                resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                availableCouponList.add(resultCouponDetail);
                break;
            case 1: // 满减券
                if (amount.compareTo(termsOfUse) >= 0) {
                    resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                    availableCouponList.add(resultCouponDetail);
                } else {
                    notAvailableCouponList.add(resultCouponDetail);
                }
                break;
            case 2: // 折扣券
                if (amount.compareTo(termsOfUse) >= 0) {
                    BigDecimal discountRate = jsonObject.getBigDecimal("discountRate");
                    BigDecimal multiply = amount.multiply(discountRate);
                    if (multiply.compareTo(maximumDiscountAmount) >= 0) {
                        resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                    } else {
                        resultCouponDetail.setCouponAmount(multiply);
                    }
                    availableCouponList.add(resultCouponDetail);
                } else {
                    notAvailableCouponList.add(resultCouponDetail);
                }
                break;
            default:
                throw new ClientException("无效的优惠券类型");
        }
    }

    /**
     * 单线程版本，好理解一些。上面的多线程就是基于这个版本演进的
     */
    public QueryCouponsRespDTO listQueryUserCouponsBySync(QueryCouponsReqDTO requestParam) {
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(String.format(USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), 0, -1);

        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(each -> redisDistributedProperties.getPrefix() + String.format(COUPON_TEMPLATE_KEY, each))
                .toList();
        List<Object> couponTemplateList = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        List<CouponTemplateQueryRespDTO> couponTemplateDTOList = JSON.parseArray(JSON.toJSONString(couponTemplateList), CouponTemplateQueryRespDTO.class);
        Map<Boolean, List<CouponTemplateQueryRespDTO>> partitioned = couponTemplateDTOList.stream()
                .collect(Collectors.partitioningBy(coupon -> StrUtil.isEmpty(coupon.getGoods())));

        // 拆分后的两个列表
        List<CouponTemplateQueryRespDTO> goodsEmptyList = partitioned.get(true); // goods 为空的列表
        List<CouponTemplateQueryRespDTO> goodsNotEmptyList = partitioned.get(false); // goods 不为空的列表

        // 针对当前订单可用/不可用的优惠券列表
        List<QueryCouponsDetailRespDTO> availableCouponList = new ArrayList<>();
        List<QueryCouponsDetailRespDTO> notAvailableCouponList = new ArrayList<>();

        goodsEmptyList.forEach(each -> {
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            QueryCouponsDetailRespDTO resultQueryCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            BigDecimal maximumDiscountAmount = jsonObject.getBigDecimal("maximumDiscountAmount");
            switch (each.getType()) {
                case 0: // 立减券
                    resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                    availableCouponList.add(resultQueryCouponDetail);
                    break;
                case 1: // 满减券
                    // orderAmount 大于或等于 termsOfUse
                    if (requestParam.getOrderAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                        availableCouponList.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCouponList.add(resultQueryCouponDetail);
                    }
                    break;
                case 2: // 折扣券
                    // orderAmount 大于或等于 termsOfUse
                    if (requestParam.getOrderAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        BigDecimal multiply = requestParam.getOrderAmount().multiply(jsonObject.getBigDecimal("discountRate"));
                        if (multiply.compareTo(maximumDiscountAmount) >= 0) {
                            resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                        } else {
                            resultQueryCouponDetail.setCouponAmount(multiply);
                        }
                        availableCouponList.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCouponList.add(resultQueryCouponDetail);
                    }
                    break;
                default:
                    throw new ClientException("无效的优惠券类型");
            }
        });

        Map<String, QueryCouponGoodsReqDTO> goodsRequestMap = requestParam.getGoodsList().stream()
                .collect(Collectors.toMap(QueryCouponGoodsReqDTO::getGoodsNumber, Function.identity(), (existing, replacement) -> existing));

        goodsNotEmptyList.forEach(each -> {
            QueryCouponGoodsReqDTO couponGoods = goodsRequestMap.get(each.getGoods());
            if (couponGoods == null) {
                notAvailableCouponList.add(BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class));
            }
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            QueryCouponsDetailRespDTO resultQueryCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            switch (each.getType()) {
                case 0: // 立减券
                    resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                    availableCouponList.add(resultQueryCouponDetail);
                    break;
                case 1: // 满减券
                    // goodsAmount 大于或等于 termsOfUse
                    if (couponGoods.getGoodsAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                        availableCouponList.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCouponList.add(resultQueryCouponDetail);
                    }
                    break;
                case 2: // 折扣券
                    // goodsAmount 大于或等于 termsOfUse
                    if (couponGoods.getGoodsAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        BigDecimal discountRate = jsonObject.getBigDecimal("discountRate");
                        resultQueryCouponDetail.setCouponAmount(couponGoods.getGoodsAmount().multiply(discountRate));
                        availableCouponList.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCouponList.add(resultQueryCouponDetail);
                    }
                    break;
                default:
                    throw new ClientException("无效的优惠券类型");
            }
        });

        // 与业内标准一致，按最终优惠力度从大到小排序
        availableCouponList.sort((c1, c2) -> c2.getCouponAmount().compareTo(c1.getCouponAmount()));

        return QueryCouponsRespDTO.builder()
                .availableCouponList(availableCouponList)
                .notAvailableCouponList(notAvailableCouponList)
                .build();
    }
}