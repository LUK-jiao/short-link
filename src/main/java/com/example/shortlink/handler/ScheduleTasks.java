package com.example.shortlink.handler;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.model.ShortLinkDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
}
