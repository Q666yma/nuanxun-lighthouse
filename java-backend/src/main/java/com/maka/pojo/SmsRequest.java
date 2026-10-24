package com.maka.pojo;

public class SmsRequest {
    private String phoneNumber;
    private String location;
    private String elderlyName;
    private String confirmationLevel;  // 添加确认级别字段
    private String contactNumber;

    // Getters 和 Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getElderlyName() {
        return elderlyName;
    }

    public void setElderlyName(String elderlyName) {
        this.elderlyName = elderlyName;
    }

    public String getConfirmationLevel() {
        return confirmationLevel;
    }

    public void setConfirmationLevel(String confirmationLevel) {
        this.confirmationLevel = confirmationLevel;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}
