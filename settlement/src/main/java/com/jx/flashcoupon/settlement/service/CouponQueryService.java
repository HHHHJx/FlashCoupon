

package com.jx.flashcoupon.settlement.service;

import com.jx.flashcoupon.settlement.dto.req.QueryCouponsReqDTO;
import com.jx.flashcoupon.settlement.dto.resp.QueryCouponsRespDTO;

/**
 * 查询用户可用优惠券列表接口
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-07-23
 */
public interface CouponQueryService {

    /**
     * 查询用户可用/不可用的优惠券列表，返回 CouponsRespDTO 对象
     *
     * @param requestParam 查询参数
     * @return 包含可用/不可用优惠券的查询结果
     */
    QueryCouponsRespDTO listQueryUserCoupons(QueryCouponsReqDTO requestParam);

    /**
     * 查询用户可用/不可用的优惠券列表，返回 CouponsRespDTO 对象
     *
     * @param requestParam 查询参数
     * @return 包含可用/不可用优惠券的查询结果
     */
    QueryCouponsRespDTO listQueryUserCouponsBySync(QueryCouponsReqDTO requestParam);
}

