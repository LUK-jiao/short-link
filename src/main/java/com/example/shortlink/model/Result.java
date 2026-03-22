package com.example.shortlink.model;

import lombok.Data;

@Data
public class Result {

    private boolean success;
    private String message;
    private Object data;

    public static Result success() {
        Result result = new Result();
        result.setSuccess(true);
        result.setMessage("Operation successful");
        return result;
    }

    public static Result success(String message) {
        Result result = new Result();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static Result successWithData(Object data) {
        Result result = new Result();
        result.setSuccess(true);
        result.setMessage("Operation successful");
        result.setData(data);
        return result;
    }

    public static Result failure(String message) {
        Result result = new Result();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
