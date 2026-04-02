package com.example.shortlink.handler;

import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.internals.AcknowledgementBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Component
@Slf4j
public class CreateShortLinkBatchHandler {

    @Autowired
    private ShortLinkService shortLinkService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${Redis.keys.shortLinkInsert.retry}")
    private String RETRY_REDIS_KEY;

    @KafkaListener(topics = "${kafka.topic.createShortLink}",groupId = "${kafka.consumer.group-id}")
    public void handler(List<ShortLink> shortLinkList, Acknowledgment ack) throws IOException {
        try{
            log.info("CreateShortLinkBatchHandler firstId={}", shortLinkList.get(0).getId());
            int res = shortLinkService.batchInsertShortLink(shortLinkList);
            ack.acknowledge();
            log.info("success insert {} shortLinks", res);
        }catch (Exception e){
            if (e instanceof RuntimeException) {
                log.error("CreateShortLinkBatchHandler RuntimeException, firstId={}", shortLinkList.get(0).getId());
                // 序列化存入 Redis，方便定时任务重试
                try {
                    String key = RETRY_REDIS_KEY;
                    // 可以使用JSON序列化
                    String value = new ObjectMapper().writeValueAsString(shortLinkList);
                    redisTemplate.opsForList().rightPush(key, value);
                    log.info("Saved batch to Redis for retry, size={}", shortLinkList.size());
                    //不抛异常，直接ack
                    ack.acknowledge();
                } catch (Exception ex) {
                    log.error("Failed to save batch to Redis", ex);
                    throw new RuntimeException(ex);
                }
            } else {
                throw new RuntimeException(e); // 其他异常继续重试
            }
        }
    }
}
