

package com.jx.flashcoupon.engine.controller;

import com.jx.flashcoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.jx.flashcoupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.jx.flashcoupon.engine.service.CouponTemplateService;
import com.jx.flashcoupon.framework.result.Result;
import com.jx.flashcoupon.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板控制层
 * 开发时间：2025-07-14
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "查询优惠券模板")
    @GetMapping("/api/engine/coupon-template/query")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.findCouponTemplate(requestParam));
    }
}
