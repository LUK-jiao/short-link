package com.example.shortlink.handler;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.model.ShortLinkDTO;
import com.example.shortlink.service.ShortLinkService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class ScheduleTasks {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Value("${Redis.keys.clickSet}")
    private String setKey;

    @Value("${Redis.keys.prefix.click}")
    private String prefix;

    @Value("${Redis.keys.shortLinkInsert.retry}")
    private String RETRY_REDIS_KEY;

    @Value("${Redis.keys.shortLinkInsert.retry.fail}")
    private String RETRY_FAIL;

    @Autowired
    private ShortLinkService shortLinkService;


    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Scheduled(fixedRate = 60000)
    public void flushClickToDB(){
        log.info("flushClickToDB started");
        Set<String> codes = redisTemplate.opsForSet().members(setKey);

        if (codes == null || codes.isEmpty()) {
            return;
        }

        for (String code : codes) {
            String value = redisTemplate.opsForValue().get(prefix + code);
            if (value == null) continue;
            Long count = Long.parseLong(value);
            ShortLink shortLink = new ShortLink();
            shortLink.setShortCode(code);
            shortLink.setClickCount(count);
            // 覆盖写
            shortLinkMapper.updateClickCount(shortLink);
            redisTemplate.opsForSet().remove(setKey,code);
        }
    }

    @Scheduled(fixedRate = 120000)
    public void retryShortLinkToDB(){
        log.info("retryShortLinkToDB started");
        while(true) {
            String valueList = redisTemplate.opsForList().leftPop(RETRY_REDIS_KEY);
            if (valueList == null) {
                break;
            }
            try {
                List<ShortLink> batch = new ObjectMapper().readValue(valueList,
                        new TypeReference<List<ShortLink>>() {
                        });
                List<ShortLink>rest = new ArrayList<>();
                int res = 0;
                for (ShortLink shortLink : batch) {
                    try {
                        shortLinkMapper.insert(shortLink);
                    } catch (Exception e) {
                        log.error("retryShortLinkToDB insert fail,id = {}", shortLink.getId(),e);
                        rest.add(shortLink);
                        continue;
                    }
                    res++;
                }
                log.info("Retry insert size={}", res);
                String restValue = new ObjectMapper().writeValueAsString(rest);
                redisTemplate.opsForList().rightPush(RETRY_FAIL, restValue);
            } catch (Exception e) {
                break; // 避免无限循环阻塞
            }
        }
    }
}
