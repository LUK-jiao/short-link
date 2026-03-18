package com.example.shortlink.service.impl;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShortLinkServiceImpl  implements ShortLinkService {

    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Override
    public ShortLink getByCode(String code) {
        return shortLinkMapper.selectByCode(code);
    }
}
