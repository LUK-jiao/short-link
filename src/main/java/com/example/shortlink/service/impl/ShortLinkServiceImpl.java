package com.example.shortlink.service.impl;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ShortLinkServiceImpl  implements ShortLinkService {

    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Autowired
    private RedisTemplate<String,String>  redisTemplate;

    @Autowired
    private Cache<String,String> shortLinkCache;

    @Override
    public String getUrlByCode(String code) {
        ShortLink shortLink =  shortLinkMapper.selectByCode(code);
        if(shortLink!=null){
            return shortLink.getLongUrl();
        }
        return null;
    }

    @Override
    public String getUrlByCodeWithCache(String code) {

        String url = shortLinkCache.getIfPresent(code);
        if (url != null) {
            return url;
        }

        url = redisTemplate.opsForValue().get(code);
        if (url != null) {
            shortLinkCache.put(code, url);
            return url;
        }

        ShortLink shortLink =  shortLinkMapper.selectByCode(code);
        if(shortLink!=null){
            shortLinkCache.put(code,shortLink.getLongUrl());
            redisTemplate.opsForValue().set(code,shortLink.getLongUrl(), Expiration.from(30L,TimeUnit.MINUTES));
            url = shortLink.getLongUrl();
            return url;
        }
        return null;
    }
}
