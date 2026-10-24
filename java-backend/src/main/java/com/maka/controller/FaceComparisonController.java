package com.maka.controller;

import com.maka.service.FaceComparisonService;
import com.maka.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/face")
public class FaceComparisonController {
    @Autowired private com.maka.service.impl.FaceComparisonServiceImpl faceService;
    @Autowired private TaskService taskService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired private com.maka.support.RoleAccess roles;
    @Autowired private com.maka.support.TaskAccess access;

    private ResponseEntity<?> failure(Exception error) {
        boolean invalid = error instanceof IllegalArgumentException;
        return ResponseEntity.status(invalid ? 400 : 503).body(Map.of(
            "success", false, "qualified", false,
            "code", invalid ? "INVALID_INPUT" : "FACE_UNAVAILABLE",
            "msg", invalid ? error.getMessage() : "人脸服务暂时不可用，请稍后重试；未完成识别"));
    }

    @PostMapping("/checkQuality")
    public ResponseEntity<?> checkQuality(@RequestParam("file") MultipartFile file, HttpSession session) {
        java.util.Map<String,Object> user=roles.requireRole(session,"family","rescuer","admin");
        if ("rescuer".equals(user.get("userType"))) roles.requireApproved(session);
        try {
            boolean qualified = faceService.checkQuality(file);
            return ResponseEntity.ok(Map.of("qualified", qualified, "msg", qualified ? "合格" : "未检测到清晰完整的人脸，请更换照片"));
        } catch (Exception error) { return failure(error); }
    }

    @PostMapping("/addFace")
    public ResponseEntity<?> addFace(@RequestParam("file") MultipartFile file,
            @RequestParam("userId") Integer taskId, @RequestParam("userName") String name, HttpSession session) {
        Object user = session.getAttribute("userId");
        roles.requireRole(session,"family");
        if (!(user instanceof String)) return ResponseEntity.status(401).body(Map.of("success", false, "msg", "请先登录"));
        if (!taskService.isTaskBelongsToFamily(taskId, (String)user))
            return ResponseEntity.status(403).body(Map.of("success", false, "msg", "只能为自己发布的任务录入照片"));
        if (!access.isActive(taskId)) return ResponseEntity.status(409).body(Map.of("success",false,"msg","任务已结束，不能继续录入照片"));
        try {
            faceService.addFace(file, String.valueOf(taskId), taskService.getTaskById(taskId).getElderName());
            if (!access.isActive(taskId))
                return ResponseEntity.status(409).body(Map.of("success", false, "msg", "任务已取消，照片录入已结束"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception error) { return failure(error); }
        finally {
            // A cancellation may commit while enrollment is in flight, even if that call times out.
            if (!access.isActive(taskId))
                jdbcTemplate.update("INSERT INTO face_cleanup_queue (task_id) VALUES (?) ON DUPLICATE KEY UPDATE task_id=VALUES(task_id)", taskId);
        }
    }

    @PostMapping(value="/compare", produces="application/json;charset=UTF-8")
    public ResponseEntity<?> compare(@RequestParam("file") MultipartFile file,@RequestParam Integer taskId,HttpSession session) {
        access.requireRecognition(taskId,session);
        try { String result=faceService.compareFace(file,taskId); access.requireRecognition(taskId,session); return ResponseEntity.ok(result); }
        catch (org.springframework.web.server.ResponseStatusException denied) { throw denied; }
        catch (Exception error) { return failure(error); }
    }

    @PostMapping(value="/compareVideo", produces="application/json;charset=UTF-8")
    public ResponseEntity<?> compareVideo(@RequestParam("videoFile") MultipartFile file,@RequestParam Integer taskId,HttpSession session) {
        access.requireRecognition(taskId,session);
        try { String result=faceService.compareFaceFromVideo(file,taskId); access.requireRecognition(taskId,session); return ResponseEntity.ok(result); }
        catch (org.springframework.web.server.ResponseStatusException denied) { throw denied; }
        catch (Exception error) { return failure(error); }
    }
}
