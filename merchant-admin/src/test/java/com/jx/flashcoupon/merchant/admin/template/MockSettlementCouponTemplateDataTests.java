

package com.jx.flashcoupon.merchant.admin.template;

import cn.hutool.core.bean.BeanUtil;
import com.jx.flashcoupon.merchant.admin.common.context.UserContext;
import com.jx.flashcoupon.merchant.admin.common.context.UserInfoDTO;
import com.jx.flashcoupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.jx.flashcoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.jx.flashcoupon.merchant.admin.service.CouponTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

/**
 * Mock 优惠券模板数据，方便分库分表均衡测试
 */
@SpringBootTest
public class MockSettlementCouponTemplateDataTests {

    @Autowired
    private CouponTemplateService couponTemplateService;

    CouponTemplateTest couponTemplateTest = new CouponTemplateTest();

    @Test
    public void mockCouponTemplateTest() {
        UserInfoDTO userInfoDTO = new UserInfoDTO("1810518709471555585", "pdd45305558318", 1810714735922956666L);
        UserContext.setUser(userInfoDTO);

        // 优惠券1
        CouponTemplateDO couponTemplateDO = couponTemplateTest.buildCouponTemplateDO(null, new BigDecimal("10"), 1, 0, null);
        couponTemplateService.createCouponTemplate(BeanUtil.toBean(couponTemplateDO, CouponTemplateSaveReqDTO.class));

        // 优惠券2
        CouponTemplateDO couponTemplateDO2 = couponTemplateTest.buildCouponTemplateDO(null, new BigDecimal("3"), 0, 0, "001");
        couponTemplateService.createCouponTemplate(BeanUtil.toBean(couponTemplateDO2, CouponTemplateSaveReqDTO.class));

        // 优惠券3
        CouponTemplateDO couponTemplateDO3 = couponTemplateTest.buildCouponTemplateDO(new BigDecimal("100"), new BigDecimal("10"), 1, 1, null);
        couponTemplateService.createCouponTemplate(BeanUtil.toBean(couponTemplateDO3, CouponTemplateSaveReqDTO.class));

        // 优惠券4
        CouponTemplateDO couponTemplateDO4 = couponTemplateTest.buildCouponTemplateDO(new BigDecimal("100"), new BigDecimal("20"), 1, 2, null);
        couponTemplateService.createCouponTemplate(BeanUtil.toBean(couponTemplateDO4, CouponTemplateSaveReqDTO.class));

        // 优惠券5
        CouponTemplateDO couponTemplateDO5 = couponTemplateTest.buildCouponTemplateDO(new BigDecimal("300"), new BigDecimal("40"), 1, 2, null);
        couponTemplateService.createCouponTemplate(BeanUtil.toBean(couponTemplateDO5, CouponTemplateSaveReqDTO.class));

        UserContext.removeUser();
    }
}
