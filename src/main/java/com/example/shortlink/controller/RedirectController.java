package com.example.shortlink.controller;

import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
public class RedirectController {

    @Autowired
    private ShortLinkService shortLinkService;

    @GetMapping("/{code}")
    public void redirect(@PathVariable String code, HttpServletResponse response) throws IOException {
        log.info("请求路径为:"+ code);
        String longUrl = shortLinkService.getUrlByCode(code);
        response.sendRedirect(longUrl);
    }

    @GetMapping("/cache/{code}")
    public void redirectWithCache(@PathVariable String code, HttpServletResponse response) throws IOException {
        log.info("请求路径为:"+ code);
        String longUrl = shortLinkService.getUrlByCodeWithCache(code);
        response.sendRedirect(longUrl);
    }

}
