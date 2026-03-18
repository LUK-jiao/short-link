package com.example.shortlink.controller;

import com.example.shortlink.model.ShortLink;
import com.example.shortlink.service.ShortLinkService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RedirectController {

    @Autowired
    private ShortLinkService shortLinkService;

    @GetMapping("/{code}")
    public void redirect(@PathVariable String code, HttpServletResponse response) throws IOException {
        ShortLink link = shortLinkService.getByCode(code);
        response.sendRedirect(code);
    }
}
