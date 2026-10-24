package com.maka.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
public class DownloadController {
    @GetMapping("/download") public ResponseEntity<?> legacyReport() {
        return ResponseEntity.status(410).body(java.util.Map.of("success",false,"message","旧报告缺少归属信息，请从自己的任务重新生成并下载"));
    }
}
