package com.example.shortlink.controller;

import com.example.shortlink.model.Result;
import com.example.shortlink.model.ShortLinkDTO;
import com.example.shortlink.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
public class CreateController {

    @Autowired
    private ShortLinkService shortLinkService;

    @PostMapping("/create")
    public Result createShortLink(@RequestBody ShortLinkDTO  shortLinkDTO){
        log.info("createShortLink:{}", shortLinkDTO);
        String shortCode = shortLinkService.createShortLink(shortLinkDTO);
        return Result.success("create short link success :"+shortCode);
    }

}
