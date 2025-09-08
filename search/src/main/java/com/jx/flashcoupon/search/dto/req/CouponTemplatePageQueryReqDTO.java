

package com.jx.flashcoupon.search.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 优惠券模板分页查询接口请求参数实体
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-10
 */
@Data
@Schema(description = "优惠券模板分页查询参数")
public class CouponTemplatePageQueryReqDTO extends Page {

    /**
     * 优惠券名称
     */
    @Schema(description = "优惠券名称")
    private String name;

    /**
     * 优惠对象 0：商品专属 1：全店通用
     */
    @Schema(description = "优惠对象 0：商品专属 1：全店通用")
    private Integer target;

    /**
     * 优惠商品编码
     */
    @Schema(description = "优惠商品编码")
    private String goods;

    /**
     * 优惠类型 0：立减券 1：满减券 2：折扣券
     */
    @Schema(description = "优惠类型 0：立减券 1：满减券 2：折扣券")
    private Integer type;
}
