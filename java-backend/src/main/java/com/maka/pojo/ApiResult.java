package com.maka.pojo;

import lombok.Data;

@Data
public class ApiResult {
    private int code;      // 0成功，401未登录，403无权限，500服务器错误等
    private String msg;    // 提示消息
    private String action; // 前端要执行的动作（reload/closeAndOpen/globalJump/none）
    private String url;    // 动作相关的url
    private String title;  // 新tab的标题（如有）
    private Object data;   // 业务数据（可选）
}