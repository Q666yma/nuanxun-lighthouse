package com.maka.controller;

import java.nio.file.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.maka.support.*;

@RestController
public class PrivateUploadController {
    @Value("${file.upload-dir}") private String uploadDir;
    @Autowired private RoleAccess roles;
    @Autowired private TaskUploads uploads;
    @GetMapping("/uploads/{kind}/{filename:.+}")
    public ResponseEntity<?> read(@PathVariable String kind,@PathVariable String filename,HttpSession session) throws Exception {
        String user=String.valueOf(roles.current(session).get("uuid"));
        if (!java.util.Set.of("img","audio").contains(kind) || !filename.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9]+")) return ResponseEntity.notFound().build();
        if (!uploads.canRead("/uploads/"+kind+"/"+filename,user)) return ResponseEntity.status(403).body(java.util.Map.of("success",false,"message","无权读取此附件"));
        Path directory=Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file=directory.resolve(kind).resolve(filename).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(directory.toRealPath())) return ResponseEntity.notFound().build();
        String mime=Files.probeContentType(file);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .contentType(mime==null?MediaType.APPLICATION_OCTET_STREAM:MediaType.parseMediaType(mime)).body(new FileSystemResource(file));
    }
}
