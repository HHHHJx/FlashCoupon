

package com.jx.flashcoupon.merchant.admin.mq;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ5.x 生产消费者单元测试

 * 开发时间：2025-07-13
 */
@Slf4j
@SpringBootTest
public final class RocketMQ5xProducerConsumerTests {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @SneakyThrows
    @Test
    public void producerSendTest() {
        SendResult sendResult = rocketMQTemplate.syncSend("TestTopic", "TestMessage");
        log.info("消息队列发送结果：{}", sendResult);
        Thread.sleep(2000); // 等待 Consumer 消费消息
    }

    @TestConfiguration
    static class RocketMQ5xConfiguration {

        @Bean
        public RocketMQ5xConsumerTests rocketMQ5xConsumerTests() {
            return new RocketMQ5xConsumerTests();
        }
    }

    @RocketMQMessageListener(
            topic = "TestTopic",
            consumerGroup = "TestTopic_CG"
    )
    static class RocketMQ5xConsumerTests implements RocketMQListener<String> {

        @Override
        public void onMessage(String message) {
            log.info("接收到消费消息：{}", message);
        }
    }
}


