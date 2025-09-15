

package com.jx.flashcoupon.settlement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 结算服务｜负责用户下单时订单金额计算功能，因和订单相关联，该服务流量较大

 */
@SpringBootApplication
@MapperScan("com.jx.flashcoupon.settlement.dao.mapper")
public class SettlementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementApplication.class, args);
    }
}
