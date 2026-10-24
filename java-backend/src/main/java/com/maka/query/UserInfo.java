package com.maka.query;

import com.maka.pojo.User;
import lombok.Data;

/**
 */
@Data
public class UserInfo {

    private Integer userId;
    private String name;
    private String gender;
    private String phone;
    private String password;
    private int rescueNum;

    public UserInfo(){}

    public UserInfo(Integer userId, String name, String gender, String phone, String password) {
        this.userId = userId;
        this.name = name;
        this.gender = gender;
        this.phone = phone;
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", phone='" + phone + '\'' +
                ", password='" + password + '\'' +
                ", rescueNum=" + rescueNum +
                '}';
    }

    public void setRescueNum(int rescueNum) {
        this.rescueNum = rescueNum;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public int getRescueNum() {
        return rescueNum;
    }
}
