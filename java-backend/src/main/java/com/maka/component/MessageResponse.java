package com.maka.component;

import org.apache.http.protocol.HTTP;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 返回消息封装
 */
@Component
public class MessageResponse {
    /**
     * 请求是否成功
     */
    private boolean ok;
    /**
     * 请求状态编码
     */
    private int code;
    /**
     * 请求响应返回消息
     */
    private String message;
    /**
     * 请求返回数据
     */
    private Object data;

    private MessageResponse(){}


    /**
     * 请求失败
     * @param e 异常对象
     * @return 封装的消息对象
     */
    public static MessageResponse error(int code,String desc){
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setOk(false);
        messageResponse.setCode(code);
        messageResponse.setMessage(desc);
        return messageResponse;
    }


    /**
     * 请求失败
     * @param e 异常对象
     * @return 封装的消息对象
     */
    public static MessageResponse userError(String desc){
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setOk(false);
        messageResponse.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        messageResponse.setMessage(desc);
        return messageResponse;
    }

    /**
     * 请求失败
     * @param e 异常对象
     * @return 封装的消息对象
     */
    public static MessageResponse systemError(String desc){
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setOk(false);
        messageResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        messageResponse.setMessage(desc);
        return messageResponse;
    }

    /**
     * 请求成功，没有返回数据
     * @return 消息对象
     */
    public static MessageResponse success(){
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setOk(true);
        messageResponse.setCode(HttpStatus.OK.value());
        messageResponse.setMessage("请求成功");
        return messageResponse;
    }


    public static MessageResponse success(Object data){
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setOk(true);
        messageResponse.setCode(HttpStatus.OK.value());
        messageResponse.setMessage("成功");
        messageResponse.setData(data);
        return messageResponse;
    }

    /**
     * 请求成功，自定义消息同时返回数据
     * @param desc 自定义消息
     * @param object 返回的数据
     * @return 消息对象
     */
    public static MessageResponse success(String desc,Object object){
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setOk(true);
        messageResponse.setCode(HttpStatus.OK.value());
        messageResponse.setMessage(desc);
        messageResponse.setData(object);
        return messageResponse;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
