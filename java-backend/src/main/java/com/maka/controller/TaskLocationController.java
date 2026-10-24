package com.maka.controller;

import com.maka.service.impl.TaskLocationService;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks/{taskId}/locations")
public class TaskLocationController {
    @Autowired private TaskLocationService service;
    @GetMapping public Map<String,Object> latest(@PathVariable Integer taskId,HttpSession session) {
        return service.latest(taskId,session);
    }
    @GetMapping("/track") public Map<String,Object> track(@PathVariable Integer taskId,
            @RequestParam String userId,@RequestParam(defaultValue="0") long beforeId,
            @RequestParam(defaultValue="1000") int limit,HttpSession session) {
        return service.track(taskId,userId,beforeId,limit,session);
    }
    @PostMapping("/sharing") public Map<String,Object> sharing(@PathVariable Integer taskId,
            @RequestBody Map<String,Object> body,HttpSession session) {
        return service.sharing(taskId,body,session);
    }
    @PostMapping public Map<String,Object> record(@PathVariable Integer taskId,
            @RequestBody Map<String,Object> body,HttpSession session) {
        return service.record(taskId,body,session);
    }
}
