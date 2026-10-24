package com.maka.controller;

import com.maka.service.impl.NotificationService;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {
    @Autowired private NotificationService service;
    @GetMapping("/api/notifications")
    public Map<String,Object> list(HttpSession session,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="false")boolean unread) {return service.list(session,page,limit,unread);}
    @PostMapping("/api/notifications/{id}/read")
    public Map<String,Object> read(HttpSession session,@PathVariable long id){return service.read(session,id);}
    @GetMapping("/api/tasks/{id}/urgent")
    public Map<String,Object> urgentList(HttpSession session,@PathVariable Integer id){return service.urgentList(session,id);}
    @PostMapping("/api/tasks/{id}/urgent")
    public Map<String,Object> urgent(HttpSession session,@PathVariable Integer id,@RequestBody Map<String,Object> body){return service.urgent(session,id,body);}
    @PostMapping("/api/tasks/{id}/notify-family")
    @SuppressWarnings("unchecked")
    public Map<String,Object> family(HttpSession session,@PathVariable Integer id,@RequestBody Map<String,Object> body){
        Map<String,Object> result=service.notifyFamily(session,id,body);
        if(Boolean.TRUE.equals(body.get("email")))for(Map<String,Object> notification:(List<Map<String,Object>>)result.get("data"))
            notification.put("emailStatus",service.deliverMail((String)session.getAttribute("userId"),((Number)notification.get("id")).longValue()));
        return result;
    }
}
