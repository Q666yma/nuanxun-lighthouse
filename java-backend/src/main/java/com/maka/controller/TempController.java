package com.maka.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
public class TempController {
    @GetMapping("/temp/createGroup") public ResponseEntity<?> obsolete() {
        return ResponseEntity.status(410).body(java.util.Map.of("success",false,"message","旧建库入口已停用，请使用管理员维护功能"));
    }
}
