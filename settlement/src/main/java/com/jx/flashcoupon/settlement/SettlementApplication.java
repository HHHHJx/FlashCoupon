

package com.jx.flashcoupon.settlement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 结算服务｜负责用户下单时订单金额计算功能，因和订单相关联，该服务流量较大
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-08
 */
@SpringBootApplication
@MapperScan("com.nageoffer.onecoupon.settlement.dao.mapper")
public class SettlementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementApplication.class, args);
    }
}
