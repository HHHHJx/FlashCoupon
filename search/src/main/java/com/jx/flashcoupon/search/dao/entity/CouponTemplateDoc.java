

package com.jx.flashcoupon.search.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * 优惠券模板Elasticsearch文档实体

 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "coupon_template")
public class CouponTemplateDoc {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * id
     */
    @Id
    @Field(type = FieldType.Long)
    private Long id;

    /**
     * 优惠券名称，参与分词与搜索
     */
    @Field(type = FieldType.Text, store = true)
    private String name;

    /**
     * 优惠券来源 0：店铺券 1：平台券
     */
    @Field(type = FieldType.Integer, index = false)
    private Integer source;

    /**
     * 优惠对象 0：商品专属 1：全店通用
     */
    @Field(type = FieldType.Integer, index = false)
    private Integer target;

    /**
     * 优惠商品编码, 不进行分词
     */
    @Field(type = FieldType.Keyword, index = false)
    private String goods;

    /**
     * 优惠类型 0：立减券 1：满减券 2：折扣券
     */
    @Field(type = FieldType.Integer, index = false)
    private Integer type;

    /**
     * 有效期开始时间
     */
    @Field(type = FieldType.Date, pattern = DATE_TIME_PATTERN, index = false)
    private Date validStartTime;

    /**
     * 有效期结束时间
     */
    @Field(type = FieldType.Date, pattern = DATE_TIME_PATTERN, index = false)
    private Date validEndTime;

    /**
     * 库存
     */
    @Field(type = FieldType.Integer, index = false)
    private Integer stock;

    /**
     * 领取规则
     */
    @Field(type = FieldType.Text, index = false)
    private String receiveRule;

    /**
     * 消耗规则
     */
    @Field(type = FieldType.Text, index = false)
    private String consumeRule;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, pattern = DATE_TIME_PATTERN, index = false)
    private Date createTime;

    /**
     * 修改时间
     */
    @Field(type = FieldType.Date, pattern = DATE_TIME_PATTERN, index = false)
    private Date updateTime;
}
