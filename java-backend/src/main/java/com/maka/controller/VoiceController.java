package com.maka.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maka.service.impl.VoiceServiceImpl;
import com.maka.service.impl.VoiceServiceImpl.VoiceOperationException;
import com.maka.service.impl.VoiceServiceImpl.VoiceMatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;

@RestController
public class VoiceController {
    private final VoiceServiceImpl voiceService;

    @Autowired
    public VoiceController(VoiceServiceImpl voiceService) { this.voiceService = voiceService; }

    @GetMapping("/voice/config")
    public Map<String, Object> configuration(HttpSession session) {
        voiceService.authorizeConfiguration(session);
        Map<String, Object> result = success();
        result.put("taskScoped", true);
        result.put("formats", Arrays.asList("MP3", "WAV", "M4A", "AAC", "OGG", "WEBM", "FLAC"));
        result.put("maxBytes", VoiceServiceImpl.MAX_UPLOAD_BYTES);
        return result;
    }

    @PostMapping("/createGroup")
    public Map<String, Object> createGroup(HttpSession session) {
        JSONObject created = JSON.parseObject(voiceService.createGroup(session));
        Map<String, Object> result = success();
        result.put("groupId", created.getString("groupId"));
        result.put("message", "声纹库已创建");
        return result;
    }

    @PostMapping("/createFeature")
    public Map<String, Object> createFeature(
            @RequestParam Integer taskId,
            @RequestParam("audioFile") MultipartFile audioFile, HttpSession session) {
        JSONObject created = JSON.parseObject(voiceService.createFeature(taskId, audioFile, session));
        Map<String, Object> result = success();
        result.put("taskId", taskId);
        result.put("featureId", created.getString("featureId"));
        result.put("message", "声纹已成功注册");
        return result;
    }

    @PostMapping("/searchFeature")
    public Map<String, Object> searchFeature(
            @RequestParam Integer taskId,
            @RequestParam("audioFile") MultipartFile audioFile, HttpSession session) {
        VoiceMatchResult match = voiceService.searchFeature(taskId, audioFile, session);
        boolean hasResult = match.hasResult();
        Map<String, Object> result = success();
        result.put("taskId", taskId);
        result.put("hasResult", hasResult);
        result.put("message", hasResult ? "声纹比对完成，请结合其他线索人工确认" : "该任务尚未录入声纹，请联系家属先录入");
        result.put("similarity", hasResult ? match.getSimilarity() : null);
        result.put("matched", hasResult && match.getSimilarity() >= 0.60);
        result.put("featureInfo", hasResult ? match.getFeatureInfo() : "");
        result.put("featureId", hasResult ? match.getFeatureId() : null);
        result.put("requiresConfirmation", true);
        return result;
    }

    @ExceptionHandler(VoiceOperationException.class)
    public ResponseEntity<Map<String, Object>> voiceError(VoiceOperationException error) {
        return ResponseEntity.status(error.getStatus()).body(failure(error.getCode(), error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> inputError(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(failure("VOICE_INPUT", error.getMessage()));
    }

    private static Map<String, Object> success() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    private static Map<String, Object> failure(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        result.put("error", message);
        return result;
    }
}
