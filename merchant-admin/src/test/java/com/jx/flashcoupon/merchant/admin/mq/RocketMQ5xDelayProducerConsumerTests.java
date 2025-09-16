

package com.jx.flashcoupon.merchant.admin.mq;

import cn.hutool.core.date.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Date;

/**
 * RocketMQ5.x 延迟生产消费者单元测试

 * 开发时间：2025-07-13
 */
@Slf4j
@SpringBootTest
public final class RocketMQ5xDelayProducerConsumerTests {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @SneakyThrows
    @Test
    public void producerSendTest() {
        // 创建消息
        Message message = new Message("TestDelayTopic", "TestMessage".getBytes());
        // 设置消息的送达时间，毫秒级 Unix 时间戳
        Long deliverTimeStamp = System.currentTimeMillis() + 4L * 1000; // 4 秒后送达
        message.setDeliverTimeMs(deliverTimeStamp);
        DefaultMQProducer defaultMQProducer = rocketMQTemplate.getProducer();
        SendResult sendResult = defaultMQProducer.send(message);
        log.info("延迟消息队列发送结果：{}", sendResult);
        log.info("延迟消息已发送，当前发送时间：{}", DateUtil.formatTime(new Date()));
        Thread.sleep(6000); // 等待 Consumer 消费消息
    }

    @TestConfiguration
    static class RocketMQ5xConfiguration {

        @Bean
        public RocketMQ5xConsumerTests rocketMQ5xConsumerTests() {
            return new RocketMQ5xConsumerTests();
        }
    }

    @RocketMQMessageListener(
            topic = "TestDelayTopic",
            consumerGroup = "TestDelayTopic_CG"
    )
    static class RocketMQ5xConsumerTests implements RocketMQListener<String> {

        @Override
        public void onMessage(String message) {
            log.info("开始消费延迟消息，当前接收时间：{}", DateUtil.formatTime(new Date()));
            log.info("接收到消费消息：{}", message);
        }
    }
}


