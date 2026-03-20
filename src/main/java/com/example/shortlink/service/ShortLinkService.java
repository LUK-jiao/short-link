package com.example.shortlink.service;

import com.example.shortlink.model.ShortLink;

public interface ShortLinkService {
    String getUrlByCode(String code);

    String getUrlByCodeWithCache(String code);

    String getUrlByCodeWithProtect(String code);
}
