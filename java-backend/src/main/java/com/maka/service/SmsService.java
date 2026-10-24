package com.maka.service;

public interface SmsService {
    /**
     * 发送任务通知短信
     * @param phoneNumber 接收短信的手机号
     * @param location 位置信息
     * @param elderlyName 老人姓名
     * @param confirmationLevel 确认级别
     * @param contactNumber 联系电话
     */
    void sendSms(String phoneNumber, String location, 
                 String elderlyName, String confirmationLevel, 
                 String contactNumber);
    
    /**
     * 发送验证码短信
     * @param phoneNumber 接收短信的手机号
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String phoneNumber);
    
    /**
     * 验证短信验证码
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verifyCode(String phoneNumber, String code);
}