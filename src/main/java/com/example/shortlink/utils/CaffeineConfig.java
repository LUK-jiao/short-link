package com.example.shortlink.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String,String> shortLinkCache() {
        return Caffeine.newBuilder()
                .maximumSize(100000L)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
