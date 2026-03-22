package com.example.shortlink.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShortCodeGenerator {

    private final int BASE = 62;

    private final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public String encode(long snowflakeId){
        StringBuilder sb = new StringBuilder();
        long num = snowflakeId;
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        String shortCode = sb.reverse().toString();
        log.info("generate shortCode : {}", shortCode);
        return shortCode;
    }

    public long decode(String shortCode){
        long num = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            char c = shortCode.charAt(i);
            if (ALPHABET.indexOf(c) >= 0) {
                num = num * BASE + ALPHABET.indexOf(c);
            }else{
                throw new IllegalArgumentException("Invalid character: " + c);
            }
        }
        return num;
    }
}
