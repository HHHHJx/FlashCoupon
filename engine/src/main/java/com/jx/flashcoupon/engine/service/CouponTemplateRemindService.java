

package com.jx.flashcoupon.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.onecoupon.engine.dao.entity.CouponTemplateRemindDO;
import com.nageoffer.onecoupon.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.nageoffer.onecoupon.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.nageoffer.onecoupon.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.nageoffer.onecoupon.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.nageoffer.onecoupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

import java.util.List;

/**
 * 优惠券预约提醒业务逻辑层
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-16
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
