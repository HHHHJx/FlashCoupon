

package com.jx.flashcoupon.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券模板失败记录数据库持久层实体
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部沟通群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-08-31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_coupon_task_fail")
public class CouponTaskFailDO {

    /**
     * id
     */
    private Long id;

    /**
     * 批量id
     */
    private Long batchId;

    /**
     * JSON字符串，存储失败原因，Excel 行数等信息
     */
    @TableField(value = "`json_object`")
    private String jsonObject;
}
