

package com.jx.flashcoupon.settlement.controller;

import com.jx.flashcoupon.framework.result.Result;
import com.jx.flashcoupon.framework.web.Results;
import com.jx.flashcoupon.settlement.dto.req.QueryCouponsReqDTO;
import com.jx.flashcoupon.settlement.dto.resp.QueryCouponsRespDTO;
import com.jx.flashcoupon.settlement.service.CouponQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查询用户优惠券控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "查询用户优惠券管理")
public class CouponQueryController {

    private final CouponQueryService couponQueryService;

    @Operation(summary = "查询用户可用/不可用优惠券列表")
    @PostMapping("/api/settlement/coupon-query")
    public Result<QueryCouponsRespDTO> listQueryCoupons(@RequestBody QueryCouponsReqDTO requestParam) {
        return Results.success(couponQueryService.listQueryUserCoupons(requestParam));
    }

    @Operation(summary = "同步查询用户可用/不可用优惠券列表")
    @PostMapping("/api/settlement/coupon-query-sync")
    public Result<QueryCouponsRespDTO> listQueryCouponsBySync(@RequestBody QueryCouponsReqDTO requestParam) {
        return Results.success(couponQueryService.listQueryUserCouponsBySync(requestParam));
    }
}
