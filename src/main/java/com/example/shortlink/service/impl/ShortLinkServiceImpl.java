package com.example.shortlink.service.impl;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShortLinkServiceImpl  implements ShortLinkService {

    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Autowired
    private RedisTemplate<String,String>  redisTemplate;

    @Autowired
    private Cache<String,String> shortLinkCache;

    @Autowired
    private RBloomFilter<String> bloomFilter;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    private DefaultRedisScript<Long> unlockScript;

    @PostConstruct
    public void init() {
        unlockScript = new DefaultRedisScript<>();
        unlockScript.setScriptText(UNLOCK_SCRIPT);
        unlockScript.setResultType(Long.class);
    }

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

    @Override
    public String getUrlByCodeWithProtect(String code) {
        //首先是布隆，然后是caffeine，然后是redis，然后是加锁，然后是双检，最后记得释放锁
        if(!bloomFilter.contains(code)){
            log.info(String.format("shortcode: %s does not exist", code));
            return null;
        }

        String url = shortLinkCache.getIfPresent(code);
        if (url != null) {
            return url;
        }

        url = redisTemplate.opsForValue().get(code);
        if (url != null) {
            shortLinkCache.put(code, url);
            return url;
        }

        String lockkey = "lock:" + code;
        String request_id = UUID.randomUUID().toString();

        try{
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockkey, request_id, Expiration.from(10L,TimeUnit.SECONDS));
            if(!locked){
                Thread.sleep(50);
                return getUrlByCodeWithProtect(code);
            }

            url = redisTemplate.opsForValue().get(code);
            if(url != null){
                return url;
            }

            ShortLink shortLink =  shortLinkMapper.selectByCode(code);
            if(shortLink!=null){
                shortLinkCache.put(code,shortLink.getLongUrl());
                redisTemplate.opsForValue().set(code,shortLink.getLongUrl(), Expiration.from(30L,TimeUnit.MINUTES));
                url = shortLink.getLongUrl();
                return url;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            Long result = redisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(lockkey),
                    request_id
            );
            if (result == 0) {
                log.warn("释放锁失败，锁可能已过期或不属于当前线程, key={}, requestId={}",
                        lockkey, request_id);
            }
        }
        return url;
    }
}
