package com.example.shortlink.utils;


import com.example.shortlink.model.ShortLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaUtils {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaTemplate<String, ShortLink> kafkaTemplateShortLink;

    public void sendClick(String mailTopic,String shortCode) {
        kafkaTemplate.send(mailTopic, shortCode);
    }

    public void sendShortLink(String mailTopic, ShortLink shortLink) {
        kafkaTemplateShortLink.send(mailTopic,shortLink);
    }
}
