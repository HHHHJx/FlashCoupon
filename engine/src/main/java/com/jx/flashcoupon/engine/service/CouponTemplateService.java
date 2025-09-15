

package com.jx.flashcoupon.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jx.flashcoupon.engine.dao.entity.CouponTemplateDO;
import com.jx.flashcoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.jx.flashcoupon.engine.dto.resp.CouponTemplateQueryRespDTO;

import java.util.List;

/**
 * 优惠券模板业务逻辑层
 * 开发时间：2025-07-14
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 查询优惠券模板
     *
     * @param requestParam 请求参数
     * @return 优惠券模板信息
     */
    CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam);

    /**
     * 根据优惠券id集合查询出券信息
     *
     * @param couponTemplateIds 优惠券id集合
     */
    List<CouponTemplateDO> listCouponTemplateByIds(List<Long> couponTemplateIds, List<Long> shopNumbers);
}
