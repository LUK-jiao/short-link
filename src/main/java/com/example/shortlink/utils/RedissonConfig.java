package com.example.shortlink.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Bean
    public RBloomFilter<String> bloomFilter() {
        RBloomFilter<String> rBloomFilter =  redissonClient.getBloomFilter("short-link");
        rBloomFilter.tryInit(1000000, 0.01);
        rBloomFilter.add("abc123");
        return rBloomFilter;
    }
}
