

package com.jx.flashcoupon.distribution.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * 数据库持久层配置类｜配置 MyBatis-Plus 相关分页插件等
 * 开发时间：2025-07-25
 */
@Configuration
public class DataBaseConfiguration {

    /**
     * MyBatis-Plus MySQL 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * MyBatis-Plus 源数据自动填充类
     */
    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    /**
     * MyBatis-Plus 源数据自动填充类
     * <p>
     * 作者：马丁
     * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
    
     */
    static class MyMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            strictInsertFill(metaObject, "createTime", Date::new, Date.class);
            strictInsertFill(metaObject, "updateTime", Date::new, Date.class);
            strictInsertFill(metaObject, "delFlag", () -> 0, Integer.class);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            strictInsertFill(metaObject, "updateTime", Date::new, Date.class);
        }
    }
}
