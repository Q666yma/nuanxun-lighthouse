package com.maka.pojo;

import org.springframework.web.multipart.MultipartFile;

public class VoiceRequest {
    private String voiceId;  // 声纹 ID
    private MultipartFile audioFile; // 上传的语音文件

    // Getters 和 Setters
    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }
}
