package com.example.shortlink.service.impl;

import com.example.shortlink.enums.ShortLinkStatus;
import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.model.ShortLinkDTO;
import com.example.shortlink.service.ShortLinkService;
import com.example.shortlink.utils.KafkaUtils;
import com.example.shortlink.utils.ShortCodeGenerator;
import com.example.shortlink.utils.SnowflakeIdConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.handler.codec.base64.Base64;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.hashids.Hashids;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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

    @Autowired
    private SnowflakeIdConfig snowflakeIdConfig;

    @Autowired
    private KafkaUtils kafkaUtils;

    @Value("${kafka.topic.click}")
    private String clickTopic;

    @Value("${kafka.topic.createShortLink}")
    private String createShortLinkTopic;



    @Autowired
    private ShortCodeGenerator shortCodeGenerator;

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
        if(!bloomFilter.contains(code)){//bloom防止缓存穿透
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

        String lockkey = "lock:" + code;//加锁防止缓存击穿
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

    @Override
    public String createShortLink(ShortLinkDTO shortLinkDTO) {
        long id = snowflakeIdConfig.nextId();
        String shortCode = shortCodeGenerator.encode(id);

        ShortLink shortLink = new ShortLink();
        shortLink.setId(id);
        shortLink.setLongUrl(shortLinkDTO.getLongUrl());
        shortLink.setShortCode(shortCode);
        //剩下的时间、状态交由mysql管理
        //添加到布隆
        bloomFilter.add(shortCode);

        try{
            shortLinkMapper.insert(shortLink);
        }catch (Exception e){
            throw new RuntimeException("createShortLink error :"  + e.getMessage());
        }
        return shortCode;
    }

    @Override
    public String updateClickCount(ShortLinkDTO shortLinkDTO) {
        String shortCode = shortLinkDTO.getShortCode();
        try{
            kafkaUtils.sendClick(clickTopic,shortCode);
        }catch (Exception e){
            throw new RuntimeException("updateClickCount error :"  + e.getMessage());
        }
        return shortCode;
    }

    @Override
    public String sendShortLink(ShortLinkDTO shortLinkDTO) {
        long id = snowflakeIdConfig.nextId();
        String shortCode = shortCodeGenerator.encode(id);

        ShortLink shortLink = new ShortLink();
        shortLink.setId(id);
        shortLink.setLongUrl(shortLinkDTO.getLongUrl());
        shortLink.setShortCode(shortCode);
        Date now = new Date();
        shortLink.setCreateTime(now);
        shortLink.setExpireTime(Date.from(now.toInstant().plus(24L, ChronoUnit.HOURS)));
        try{
            kafkaUtils.sendShortLink(createShortLinkTopic,shortLink);
        }catch (Exception e){
            throw new RuntimeException("sendShortLink error :"  + e.getMessage());
        }
        return shortCode;
    }

    @Override
    public int batchInsertShortLink(List<ShortLink> msgs) {
        msgs.forEach(shortLink -> {
            bloomFilter.add(shortLink.getShortCode());
        });
        try{
            shortLinkMapper.insertBatch(msgs);
        }catch (Exception e){
            throw new RuntimeException("batchInsertShortLink error :"  + e.getMessage());
        }
        return msgs.size();
    }
}
