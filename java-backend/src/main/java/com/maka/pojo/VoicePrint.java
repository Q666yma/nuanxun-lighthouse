package com.maka.pojo;

public class VoicePrint {
    private Long id;
    private String userId; // 用户 ID
    private String voiceId; // 声纹 ID
    private String voiceFeature; // 声纹特征数据

    // Getters 和 Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public String getVoiceFeature() {
        return voiceFeature;
    }

    public void setVoiceFeature(String voiceFeature) {
        this.voiceFeature = voiceFeature;
    }
}
