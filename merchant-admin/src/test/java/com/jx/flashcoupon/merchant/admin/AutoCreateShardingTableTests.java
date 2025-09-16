

package com.jx.flashcoupon.merchant.admin;

import org.junit.jupiter.api.Test;

/**
 * 创建优惠券项目中需要分片数据库表 SQL 语句

 * 开发时间：2025-07-10
 */
public class AutoCreateShardingTableTests {

    private final String couponTable = "CREATE TABLE `t_coupon_template_%d` (\n" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "  `name` varchar(256) DEFAULT NULL COMMENT '优惠券名称',\n" +
            "  `shop_number` bigint(20) DEFAULT NULL COMMENT '店铺编号',\n" +
            "  `source` tinyint(1) DEFAULT NULL COMMENT '优惠券来源 0：店铺券 1：平台券',\n" +
            "  `target` tinyint(1) DEFAULT NULL COMMENT '优惠对象 0：商品专属 1：全店通用',\n" +
            "  `goods` text COMMENT '优惠商品编码',\n" +
            "  `type` tinyint(1) DEFAULT NULL COMMENT '优惠类型 0：立减券 1：满减券 2：折扣券',\n" +
            "  `valid_start_time` datetime DEFAULT NULL COMMENT '有效期开始时间',\n" +
            "  `valid_end_time` datetime DEFAULT NULL COMMENT '有效期结束时间',\n" +
            "  `stock` int(11) DEFAULT NULL COMMENT '库存',\n" +
            "  `receive_rule` json DEFAULT NULL COMMENT '领取规则',\n" +
            "  `consume_rule` json DEFAULT NULL COMMENT '消耗规则',\n" +
            "  `status` tinyint(1) DEFAULT NULL COMMENT '优惠券状态 0：生效中 1：已结束',\n" +
            "  `create_time` datetime DEFAULT NULL COMMENT '创建时间',\n" +
            "  `update_time` datetime DEFAULT NULL COMMENT '修改时间',\n" +
            "  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  KEY `idx_shop_number` (`shop_number`) USING BTREE\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';";

    private final String couponLogTable = "CREATE TABLE `t_coupon_template_log_%d` (\n" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "  `shop_number` bigint(20) DEFAULT NULL COMMENT '店铺编号',\n" +
            "  `coupon_template_id` bigint(20) DEFAULT NULL COMMENT '优惠券模板ID',\n" +
            "  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人',\n" +
            "  `operation_log` text COMMENT '操作日志',\n" +
            "  `original_data` varchar(1024) DEFAULT NULL COMMENT '原始数据',\n" +
            "  `modified_data` varchar(1024) DEFAULT NULL COMMENT '修改后数据',\n" +
            "  `create_time` datetime DEFAULT NULL COMMENT '创建时间',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  KEY `idx_shop_number` (`shop_number`) USING BTREE\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板操作日志表';";

    @Test
    public void autoCreateCouponTemplateShardingTable() {
        for (int i = 0; i < 16; i++) {
            System.out.println(String.format(couponTable, i));
        }
    }

    @Test
    public void autoCreateCouponTemplateLogShardingTable() {
        for (int i = 0; i < 16; i++) {
            System.out.println(String.format(couponLogTable, i));
        }
    }
}
