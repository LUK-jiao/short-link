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

@DubboService
@Slf4j
public class ShortLinkApiImpl implements ShortLinkApi {

    @Autowired
    private ShortLinkService shortLinkService;

    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Override
    public Result<ShortLinkResultDTO> createShortLink(ShortLinkCreateDTO shortLinkCreateDTO) {
        log.info("ShortLinkApi.createShortLink: {}", shortLinkCreateDTO);

        String longUrl = shortLinkCreateDTO.getLongUrl();
        ShortLinkDTO shortLinkDTO = new ShortLinkDTO();
        shortLinkDTO.setLongUrl(longUrl);

        try {
            String shortCode = shortLinkService.createShortLink(shortLinkDTO);

            ShortLinkResultDTO resultDTO = new ShortLinkResultDTO();
            resultDTO.setShortCode(shortCode);
            resultDTO.setLongUrl(longUrl);
            resultDTO.setSuccess(true);

            log.info("ShortLinkApi.createShortLink result: {}", resultDTO);
            return Result.success(resultDTO);
        } catch (Exception e) {
            log.error("ShortLinkApi.createShortLink error: {}", e.getMessage(), e);

            ShortLinkResultDTO resultDTO = new ShortLinkResultDTO();
            resultDTO.setSuccess(false);
            resultDTO.setErrorMessage(e.getMessage());

            return Result.fail(resultDTO.getErrorMessage());
        }
    }

    @Override
    public Result<String> getUrlByCode(String shortCode) {
        log.info("ShortLinkApi.getUrlByCode shortCode: {}", shortCode);

        ShortLink shortLink = shortLinkMapper.selectByCode(shortCode);
        if (shortLink == null) {
            log.warn("ShortLinkApi.getUrlByCode: shortCode not found");
            return Result.fail("短链不存在");
        }

        log.info("ShortLinkApi.getUrlByCode longUrl: {}", shortLink.getLongUrl());
        return Result.success(shortLink.getLongUrl());
    }

    @Override
    public Result<Boolean> exists(String shortCode) {
        log.info("ShortLinkApi.exists shortCode: {}", shortCode);

        ShortLink shortLink = shortLinkMapper.selectByCode(shortCode);
        boolean exists = shortLink != null;

        log.info("ShortLinkApi.exists result: {}", exists);
        return Result.success(exists);
    }
}