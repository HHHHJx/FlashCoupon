

package com.jx.flashcoupon.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券模板查询接口请求参数实体
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优惠券模板查询请求参数实体")
public class CouponTemplateQueryReqDTO {

    /**
     * 店铺编号
     */
    @Schema(description = "店铺编号", example = "1810714735922956666", required = true)
    private String shopNumber;

    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id", example = "1810966706881941507", required = true)
    private String couponTemplateId;
}
