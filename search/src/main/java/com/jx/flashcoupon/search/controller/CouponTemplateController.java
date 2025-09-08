

package com.jx.flashcoupon.search.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.onecoupon.framework.result.Result;
import com.nageoffer.onecoupon.framework.web.Results;
import com.nageoffer.onecoupon.search.dto.req.CouponTemplatePageQueryReqDTO;
import com.nageoffer.onecoupon.search.dto.resp.CouponTemplatePageQueryRespDTO;
import com.nageoffer.onecoupon.search.service.CouponTemplateSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板搜索控制层
 * <p>
 * 作者：蛋仔
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-02
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
