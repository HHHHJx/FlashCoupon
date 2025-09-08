

package com.jx.flashcoupon.settlement.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询用户优惠券响应参数
 * <p>
 * 作者：Henry Wan
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "查询用户优惠券响应参数")
public class QueryCouponsRespDTO {

    /**
     * 可用优惠券列表
     */
    @Schema(description = "可用优惠券列表")
    private List<QueryCouponsDetailRespDTO> availableCouponList;

    /**
     * 不可用优惠券列表
     */
    @Schema(description = "不可用优惠券列表")
    private List<QueryCouponsDetailRespDTO> notAvailableCouponList;
}
