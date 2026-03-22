package com.example.shortlink.service;

import com.example.shortlink.model.ShortLinkDTO;

import java.util.List;

public interface ClickCountWriteService {

    public void updateRedisClick(List<String> messages);
}
