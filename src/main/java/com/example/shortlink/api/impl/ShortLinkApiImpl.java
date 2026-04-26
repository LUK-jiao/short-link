package com.example.shortlink.api.impl;

import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.model.ShortLink;
import com.example.shortlink.model.ShortLinkDTO;
import com.example.shortlink.service.ShortLinkService;
import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.shortlink.ShortLinkCreateDTO;
import com.linkflow.api.dto.shortlink.ShortLinkResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DubboService
@Slf4j
public class ShortLinkApiImpl implements ShortLinkApi {

    @Autowired
    private ShortLinkService shortLinkService;

    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Override
    public Result<ShortLinkResultDTO> createShortLink(ShortLinkCreateDTO shortLinkCreateDTO) {
        log.info("ShortLinkApi.createShortLink:{}", shortLinkCreateDTO);
        String longUrl = shortLinkCreateDTO.getLongUrl();
        ShortLinkDTO shortLinkDTO = new ShortLinkDTO();
        ShortLinkResultDTO shortLinkResultDTO = new ShortLinkResultDTO();
        shortLinkDTO.setLongUrl(longUrl);

        try{
            String shortCode = shortLinkService.createShortLink(shortLinkDTO);
            shortLinkResultDTO.setShortCode(shortCode);
            shortLinkResultDTO.setSuccess(true);
            shortLinkResultDTO.setLongUrl(shortLinkCreateDTO.getLongUrl());
        } catch(Exception e){
            log.error("ShortLinkApi.createShortLink error :{}",e.getMessage());
            shortLinkResultDTO.setSuccess(false);
            shortLinkResultDTO.setErrorMessage(e.getMessage());
        }
        log.info("ShortLinkApi.createShortLink return :{}",shortLinkResultDTO);
        return  Result.success(shortLinkResultDTO);
    }

    @Override
    public Result<String> getUrlByCode(String shortCode) {
        log.info("ShortLinkApi.getUrlByCode shortCode = {}", shortCode);
        ShortLink shortLink = shortLinkMapper.selectByCode(shortCode);
        if(shortLink == null){
            return Result.fail("no such record");
        }
        log.info("ShortLinkApi.getUrlByCode longUrl = {}", shortLink.getLongUrl());
        return Result.success(shortLink.getLongUrl());
    }

    @Override
    public Result<Boolean> exists(String s) {
        return null; //TODO 这个托7的
    }
}
