package com.jx.flashcoupon.merchant.admin.template;

import com.alibaba.fastjson2.JSON;
import com.jx.flashcoupon.merchant.admin.common.context.UserContext;
import com.jx.flashcoupon.merchant.admin.common.context.UserInfoDTO;
import com.jx.flashcoupon.merchant.admin.controller.CouponTemplateController;
import com.jx.flashcoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 优惠券模板防重复提交测试
 */
@Slf4j
@SpringBootTest
public class CouponTemplateCreateDuplicateSubmitTests {

    @Autowired
    private CouponTemplateController couponTemplateController;

    @SneakyThrows
    @Test
    public void testDuplicateSubmit() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        String paramJSONStr = """
                {
                  "name": "用户下单满10减3特大优惠",
                  "source": 0,
                  "target": 1,
                  "goods": "",
                  "type": 0,
                  "validStartTime": "2024-07-08 12:00:00",
                  "validEndTime": "2024-08-17 12:00:00",
                  "stock": 20990,
                  "receiveRule": "{\\"limitPerPerson\\":10,\\"usageInstructions\\":\\"3\\"}",
                  "consumeRule": "{\\"termsOfUse\\":10,\\"maximumDiscountAmount\\":3,\\"explanationOfUnmetConditions\\":\\"3\\",\\"validityPeriod\\":\\"48\\"}"
                }
                """;

        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);

        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                RequestContextHolder.setRequestAttributes(attributes);
                UserInfoDTO userInfoDTO = new UserInfoDTO("156415613", "jd123", (long)45612321);
                UserContext.setUser(userInfoDTO);
                // 将 ServletRequestAttributes 绑定到当前线程
                try {
                    couponTemplateController.createCouponTemplate(JSON.parseObject(paramJSONStr, CouponTemplateSaveReqDTO.class));
                } catch (Exception ex) {
                    log.error("新增优惠券模板异常", ex);
                } finally {
                    RequestContextHolder.resetRequestAttributes(); // 确保当前线程中的 RequestAttributes 被清除
                }
            });
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(1000);
        }
    }
}