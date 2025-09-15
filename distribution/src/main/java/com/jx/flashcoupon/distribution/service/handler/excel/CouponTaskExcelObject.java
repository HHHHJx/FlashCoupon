

package com.jx.flashcoupon.distribution.service.handler.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 优惠券推送任务 Excel 元数据实体

 * 开发时间：2025-07-14
 */
@Data
public class CouponTaskExcelObject {

    @ExcelProperty("用户ID")
    private String userId;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String mail;
}
