package com.example.shortlink.service;

import com.example.shortlink.model.ShortLink;
import com.example.shortlink.model.ShortLinkDTO;

public interface ShortLinkService {
    String getUrlByCode(String code);

    String getUrlByCodeWithCache(String code);

    String getUrlByCodeWithProtect(String code);

    String createShortLink(ShortLinkDTO shortLinkDTO);
}
