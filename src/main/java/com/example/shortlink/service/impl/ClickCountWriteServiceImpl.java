package com.example.shortlink.service.impl;

import com.example.shortlink.model.ShortLinkDTO;
import com.example.shortlink.service.ClickCountWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClickCountWriteServiceImpl implements ClickCountWriteService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Value("${Redis.keys.clickSet}")
    private String redis_set_key;

    @Value("${Redis.keys.prefix.click}")
    private String prefix;

    @Override
    public void updateRedisClick(List<String> shortCodes) {
        Map<String, Long> counter = shortCodes.stream()
                .collect(Collectors.groupingBy(
                        code -> code,
                        Collectors.counting()
                ));

        for(String code : counter.keySet()){
            redisTemplate.opsForValue().increment(prefix + code, counter.get(code));
            redisTemplate.opsForSet().add(redis_set_key, code);
        }
    }
}
