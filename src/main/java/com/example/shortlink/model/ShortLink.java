package com.example.shortlink.model;

import lombok.Data;

import java.util.Date;

@Data
public class ShortLink {
    private Long id;

    private String shortCode;

    private String longUrl;

    private Date createTime;

    private Date expireTime;

    private Byte status;
}