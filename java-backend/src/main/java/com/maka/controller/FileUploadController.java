package com.maka.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/upload")
public class FileUploadController {
    @Value("${file.upload-dir}") private String uploadDir;
    @Value("${file.access-prefix:/uploads/}") private String accessPrefix;
    @org.springframework.beans.factory.annotation.Autowired private com.maka.support.RoleAccess roles;
    @org.springframework.beans.factory.annotation.Autowired private com.maka.support.TaskUploads uploads;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, javax.servlet.http.HttpSession session) throws Exception {
        return save(file, true, session);
    }
    @PostMapping("/audio")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file, javax.servlet.http.HttpSession session) throws Exception {
        return save(file, false, session);
    }

    private ResponseEntity<?> invalid(String message) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "code", 400, "msg", message));
    }

    private ResponseEntity<?> save(MultipartFile file, boolean image, javax.servlet.http.HttpSession session) throws Exception {
        Map<String,Object> user = roles.requireRole(session,"family","rescuer","admin");
        if ("rescuer".equals(user.get("userType"))) roles.requireApproved(session);
        if (file.isEmpty()) return invalid("请选择要上传的文件");
        if (file.getSize() > (image ? 10L : 20L)*1024*1024) return invalid(image ? "图片不能超过 10 MB" : "语音不能超过 20 MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot+1).toLowerCase(Locale.ROOT);
        Set<String> allowed = image ? Set.of("jpg","jpeg","png","gif","bmp") : Set.of("wav","mp3","ogg","m4a","webm","aac","mp4");
        if (!allowed.contains(ext)) return invalid(image ? "请上传 JPG、PNG、GIF 或 BMP 图片" : "不支持此音频格式");
        if (image) {
            try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
                if (input == null) return invalid("图片无法读取");
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return invalid("文件不是有效的图片");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    if ((long)reader.getWidth(0)*reader.getHeight(0) > 40_000_000L)
                        return invalid("图片尺寸过大，请缩小后上传");
                } finally { reader.dispose(); }
            } catch (java.io.IOException error) { return invalid("图片损坏，请更换文件"); }
        }
        Path directory = Paths.get(uploadDir, image ? "img" : "audio");
        Files.createDirectories(directory);
        String saved = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        file.transferTo(directory.resolve(saved));
        String prefix = accessPrefix.endsWith("/") ? accessPrefix : accessPrefix + "/";
        String url = prefix + (image ? "img/" : "audio/") + saved;
        try { uploads.register(url,String.valueOf(user.get("uuid"))); }
        catch (RuntimeException failure) { Files.deleteIfExists(directory.resolve(saved)); throw failure; }
        return ResponseEntity.ok(Map.of("success", true, "code", 0, "url", url));
    }
}
