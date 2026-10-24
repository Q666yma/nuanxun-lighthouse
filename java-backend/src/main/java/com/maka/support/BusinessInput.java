package com.maka.support;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class BusinessInput {
    private BusinessInput() {}
    public static String text(Map<String,?> body, String key, int max) {
        Object value = body.get(key);
        if (!(value instanceof String) || ((String)value).trim().isEmpty() || ((String)value).length()>max)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请填写有效的"+key);
        return ((String)value).trim();
    }
    public static String requestId(Map<String,?> body) {
        String id=text(body,"requestId",100);
        if (!id.matches("[A-Za-z0-9._:-]{8,100}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"提交标识无效");
        return id;
    }
    public static int id(Object value) {
        try { int id=Integer.parseInt(String.valueOf(value)); if(id>0)return id; } catch(Exception ignored){}
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择有效任务");
    }
    public static int page(int page) { return Math.max(1,Math.min(100000,page)); }
    public static int limit(int limit) { return Math.max(1,Math.min(100,limit)); }
}
