package com.maka.dto;

public class UserRegistrationRequest {
    private String username;
    private String password;
    private String phone;
    private String verificationCode;
    private String gender;
    private String userType;
    private String name;       // 真实姓名
    private String location;   // 仅志愿者需要

    // ===== 新增邮箱相关字段 =====
    private String email;      // 注册邮箱
    private String emailCode;  // 邮箱验证码

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmailCode() { return emailCode; }
    public void setEmailCode(String emailCode) { this.emailCode = emailCode; }
}