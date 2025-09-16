

package com.jx.flashcoupon.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jx.flashcoupon.engine.dao.entity.CouponTemplateRemindDO;
import com.jx.flashcoupon.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.jx.flashcoupon.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.jx.flashcoupon.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.jx.flashcoupon.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.jx.flashcoupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

import java.util.List;

/**
 * 优惠券预约提醒业务逻辑层

 * 开发时间：2025-07-16
 */
public interface CouponTemplateRemindService extends IService<CouponTemplateRemindDO> {

    /**
     * 创建抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    void createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam);

    /**
     * 分页查询抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    List<CouponTemplateRemindQueryRespDTO> listCouponRemind(CouponTemplateRemindQueryReqDTO requestParam);

    /**
     * 取消抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    void cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam);

    /**
     * 检查是否取消抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    boolean isCancelRemind(CouponTemplateRemindDTO requestParam);
}
