package com.example.shortlink.enums;

public enum ShortLinkStatus {

    VALID("有效",1),
    IN_VALID("无效",0);

    private final String desc;

    private final Integer code;

    ShortLinkStatus(String desc, Integer code){
        this.desc = desc;
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getCode() {
        return code;
    }
}
