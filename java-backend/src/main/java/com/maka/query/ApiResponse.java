package com.maka.query;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;

    // 构造器/getter/setter省略
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(0);
        r.setMsg("success");
        r.setData(data);
        return r;
    }
    public static <T> ApiResponse<T> error(String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(1);
        r.setMsg(msg);
        return r;
    }
}