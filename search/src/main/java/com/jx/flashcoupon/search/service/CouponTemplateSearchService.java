

package com.jx.flashcoupon.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.onecoupon.search.dto.req.CouponTemplatePageQueryReqDTO;
import com.nageoffer.onecoupon.search.dto.resp.CouponTemplatePageQueryRespDTO;

/**
 * 优惠券模板搜索业务逻辑层
 * <p>
 * 作者：蛋仔
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-02
 */
public interface CouponTemplateSearchService {

    /**
     * 分页查询商家优惠券模板
     *
     * @param requestParam 请求参数
     * @return 商家优惠券模板分页数据
     */
    IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam);
}
