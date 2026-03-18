package com.example.shortlink.service;

import com.example.shortlink.model.ShortLink;

public interface ShortLinkService {
    ShortLink getByCode(String code);
}
