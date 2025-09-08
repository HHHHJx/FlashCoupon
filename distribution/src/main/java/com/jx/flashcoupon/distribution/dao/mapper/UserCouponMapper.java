

package com.jx.flashcoupon.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.onecoupon.distribution.dao.entity.UserCouponDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户优惠券数据库持久层
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-14
 */
public interface UserCouponMapper extends BaseMapper<UserCouponDO> {

    /**
     * 查询用户优惠券最大领取次数
     * 设计用户优惠券发放时考虑使用该方法，后来经过重构舍弃了该流程，等待后续看是否有机会重启
     *
     * @param couponTemplateId 优惠券模板 ID
     * @param userIds          用户 ID 集合
     * @return 用户优惠券最大领取次数集合
     */
    @Deprecated
    List<UserCouponDO> selectUserCourseMaxReceiveCount(@Param("couponTemplateId") Long couponTemplateId, @Param("userIds") List<Long> userIds);
}
