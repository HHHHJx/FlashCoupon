

package com.jx.flashcoupon.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询抢券预约提醒接口请求参数实体
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-07-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询优惠券预约抢券提醒参数实体")
public class CouponTemplateRemindQueryReqDTO {

    /**
     * 用户id
     */
    @Schema(description = "用户id", example = "1810518709471555585", required = true)
    private String userId;
}
