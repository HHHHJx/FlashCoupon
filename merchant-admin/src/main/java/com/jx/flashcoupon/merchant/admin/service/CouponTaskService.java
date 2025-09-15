

package com.jx.flashcoupon.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jx.flashcoupon.merchant.admin.dao.entity.CouponTaskDO;
import com.jx.flashcoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.jx.flashcoupon.merchant.admin.dto.req.CouponTaskPageQueryReqDTO;
import com.jx.flashcoupon.merchant.admin.dto.resp.CouponTaskPageQueryRespDTO;
import com.jx.flashcoupon.merchant.admin.dto.resp.CouponTaskQueryRespDTO;

/**
 * 优惠券推送业务逻辑层
 */
public interface CouponTaskService extends IService<CouponTaskDO> {

    /**
     * 商家创建优惠券推送任务
     *
     * @param requestParam 请求参数
     */
    void createCouponTask(CouponTaskCreateReqDTO requestParam);

    /**
     * 分页查询商家优惠券推送任务
     *
     * @param requestParam 请求参数
     * @return 商家优惠券推送任务分页数据
     */
    IPage<CouponTaskPageQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam);

    /**
     * 查询优惠券推送任务详情
     *
     * @param taskId 推送任务 ID
     * @return 优惠券推送任务详情
     */
    CouponTaskQueryRespDTO findCouponTaskById(String taskId);
}
