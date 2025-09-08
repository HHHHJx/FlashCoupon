package com.jx.flashcoupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jx.flashcoupon.mapper")
public class FlashCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashCouponApplication.class, args);
    }

}
