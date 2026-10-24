package com.maka.error;

import com.maka.component.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * 全局异常处理
 */
@ControllerAdvice
public class WebExceptionHandler {

    // private Logger logger = LoggerFactory.getLogger("WebExceptionHandler");


    // /**
    //  * 处理数据校验异常 MethodArgumentNotValidException
    //  * @param ex 校验异常
    //  * @return 提示信息
    //  */
    // @ExceptionHandler(MethodArgumentNotValidException.class)
    // @ResponseBody
    // public MessageResponse handleBindException(MethodArgumentNotValidException ex) {
    //     FieldError fieldError = ex.getBindingResult().getFieldError();
    //     if(fieldError!=null){
    //         return MessageResponse.userError(fieldError.getDefaultMessage());
    //     }
    //     return MessageResponse.systemError("内部错误");
    // }

    // /**
    //  * 处理数据校验异常 BindException
    //  * @param e 校验异常
    //  * @return 提示信息
    //  */
    // @ExceptionHandler(BindException.class)
    // @ResponseBody
    // public MessageResponse handleBindException(BindException e){
    //     FieldError fieldError = e.getBindingResult().getFieldError();
    //     if(fieldError!=null){
    //         return MessageResponse.userError(fieldError.getDefaultMessage());
    //     }
    //     return MessageResponse.systemError("请稍后再试");
    // }


    // @ExceptionHandler(IOException.class)
    // @ResponseBody
    // public MessageResponse handlerIoException(Exception e){
    //     e.printStackTrace();
    //     return MessageResponse.systemError("服务器出现异常，请稍后在世");
    // }


    // @ExceptionHandler(Exception.class)
    // @ResponseBody
    // public MessageResponse handledException(Exception exception){
    //     exception.printStackTrace();
    //     return MessageResponse.systemError("未知");
    // }






}
