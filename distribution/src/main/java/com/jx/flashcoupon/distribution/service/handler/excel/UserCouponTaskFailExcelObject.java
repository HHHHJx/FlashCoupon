

package com.jx.flashcoupon.distribution.service.handler.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户优惠券分发失败记录写入 Excel
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCouponTaskFailExcelObject {

    @ColumnWidth(20)
    @ExcelProperty("行数")
    private Integer rowNum;

    @ColumnWidth(30)
    @ExcelProperty("错误原因")
    private String cause;
}
