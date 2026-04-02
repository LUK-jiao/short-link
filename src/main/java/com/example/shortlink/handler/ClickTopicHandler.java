package com.example.shortlink.handler;

import com.example.shortlink.service.ClickCountWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class ClickTopicHandler {

    @Autowired
    private ClickCountWriteService clickCountWriteService;

    @KafkaListener(topics = "${kafka.topic.click}",groupId = "${kafka.consumer.group-id}" )
    @Transactional
    public void handler(List<String> messages, Acknowledgment ack) {
        log.info("ClickTopicHandler: received messages-sizes: {}", messages.size());
        try{
            clickCountWriteService.updateRedisClick(messages);
            ack.acknowledge();
        } catch (Exception e) {
            throw new RuntimeException(e);//自动重试
        }

    }
}
