

package com.jx.flashcoupon.search.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jx.flashcoupon.framework.result.Result;
import com.jx.flashcoupon.framework.web.Results;
import com.jx.flashcoupon.search.dto.req.CouponTemplatePageQueryReqDTO;
import com.jx.flashcoupon.search.dto.resp.CouponTemplatePageQueryRespDTO;
import com.jx.flashcoupon.search.service.CouponTemplateSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板搜索控制层

 * 开发时间：2025-08-02
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模板搜索管理")
public class CouponTemplateController {

    private final CouponTemplateSearchService couponTemplateSearchService;

    @Operation(summary = "分页查询优惠券模板")
    @GetMapping("/api/search/coupon-template/page")
    public Result<IPage<CouponTemplatePageQueryRespDTO>> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        return Results.success(couponTemplateSearchService.pageQueryCouponTemplate(requestParam));
    }
}
