package com.example.shortlink.handler;

import com.example.shortlink.service.ClickCountWriteService;
import io.lettuce.core.dynamic.annotation.Key;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.internals.AcknowledgementBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
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
    public void handler(List<String> messages) {
        log.info("ClickTopicHandler: received messages-sizes: {}", messages.size());
        try{
            clickCountWriteService.updateRedisClick(messages);
        } catch (Exception e) {
            throw new RuntimeException(e);//自动重试
        }

    }
}
