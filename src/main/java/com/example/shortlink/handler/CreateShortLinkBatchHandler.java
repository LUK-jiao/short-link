package com.example.shortlink.handler;

import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class CreateShortLinkBatchHandler {

    @Autowired
    private ShortLinkService shortLinkService;

    @KafkaListener(topics = "${kafka.topic.createShortLink}",groupId = "${kafka.consumer.group-id}")
    @Transactional
    public void handler(List<ShortLink> shortLinkList) throws IOException {
        try{
            shortLinkService.batchInsertShortLink(shortLinkList);
            log.info("success insert {} shortLinks", shortLinkList.size());
        }catch (Exception e){
            throw new RuntimeException(e);//自动重试
        }

    }
}
