package com.maka.service.impl;

import com.maka.service.SmsService;
import com.maka.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private MailService mailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 走失人员通知收件邮箱

    @Override
    public void sendSms(String phoneNumber,String location,String elderlyName,String confirmationLevel,String contactNumber) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,"旧的无任务通知已停用，请从任务中联系绑定家属");
    }

    @Override
    public boolean sendVerificationCode(String email) {
        try {
            // 生成6位随机验证码
            String code = generateRandomCode();

            // 将验证码存入Redis，设置5分钟过期
            String key = "sms:verification:" + email;
            redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

            // 发送验证码邮件
            String subject = "【暖寻灯塔】您的验证码";
            String body = "您好！\n\n"
                    + "您的验证码为：" + code + "\n\n"
                    + "验证码5分钟内有效，请勿泄露给他人。\n\n"
                    + "如非本人操作，请忽略此邮件。\n\n"
                    + "——暖寻灯塔团队";

            mailService.sendEmail(email, subject, body);
            System.out.println("验证码邮件发送成功！");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("验证码邮件发送时出现异常：" + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String key = "sms:verification:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
