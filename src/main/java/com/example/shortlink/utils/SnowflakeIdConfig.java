package com.example.shortlink.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SnowflakeIdConfig {

    private final long START_TIMESTAMP = 1577808000000L;

    private final long MACHINE_ID_BITS = 10L;
    private final long SEQUENCE_BITS = 12L;

    private final long MAX_MACHINE_ID = ~(-1L<<MACHINE_ID_BITS);
    private final long MAX_SEQUENCE = ~(-1L<<SEQUENCE_BITS);

    private final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private final long MACHINE_SHIFT = SEQUENCE_BITS;

    private final long machineId;

    // 序列号（0-4095）
    private long sequence = 0L;

    // 上次生成ID的时间戳
    private long lastTimeStamp = -1L;

    public SnowflakeIdConfig(@Value("${snowflake.worker-id}") long machineId) {
        // 校验机器ID范围
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                    String.format("machineId must be between 0 and %d, but got %d",
                            MAX_MACHINE_ID, machineId));
        }
        this.machineId = machineId;
        log.info("SnowflakeIdGenerator 初始化完成，machineId={}", machineId);
    }

    public synchronized long nextId() {
        long currentTimeStamp = System.currentTimeMillis();
        //判断是否有时钟回拨
        if (currentTimeStamp < lastTimeStamp) {
            long offset = lastTimeStamp - currentTimeStamp;
            if (offset <= 5L) {
                currentTimeStamp = waitUntil(lastTimeStamp);
            } else {
                throw new RuntimeException("Clock moved backwards too much");
            }
        }
        //同一毫秒
        if (currentTimeStamp == lastTimeStamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                currentTimeStamp = waitTillNextMillis(lastTimeStamp);
            }
        }else{
            sequence = 0L;
        }

        lastTimeStamp = currentTimeStamp;

        return ((currentTimeStamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequence;
    }

    private long waitTillNextMillis(long lastTimeStamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimeStamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private long waitUntil(long target) {
        long now = System.currentTimeMillis();
        while (now < target) {
            now = System.currentTimeMillis();
        }
        return now;
    }
}
