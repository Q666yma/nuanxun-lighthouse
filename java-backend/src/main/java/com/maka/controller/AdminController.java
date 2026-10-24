package com.maka.controller;

import com.maka.support.RoleAccess;
import com.maka.support.BusinessInput;
import com.maka.support.AdminTaskActions;
import com.maka.service.impl.AdminService;
import com.maka.service.impl.ContentService;
import com.maka.service.impl.NotificationService;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired private RoleAccess access;
    @Autowired private AdminService service;
    @Autowired private ContentService content;
    @Autowired private NotificationService notifications;
    @Autowired private AdminTaskActions tasks;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Environment environment;
    private Map<String,Object> admin(HttpSession session){return access.requireRole(session,"admin");}
    private String actor(HttpSession session){return (String)admin(session).get("uuid");}
    @GetMapping("/summary") public Map<String,Object> summary(HttpSession session){admin(session);return service.summary();}
    @GetMapping({"/users","/rescuers","/tasks","/urgent","/notifications","/audit"})
    public Map<String,Object> list(HttpSession session,javax.servlet.http.HttpServletRequest request,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit){
        admin(session);String path=request.getRequestURI();return service.list(path.substring(path.lastIndexOf('/')+1),status,page,limit);
    }
    @PostMapping("/users/{uuid}/status") public Map<String,Object> status(HttpSession session,@PathVariable String uuid,@RequestBody Map<String,Object> body){return service.status(actor(session),uuid,body);}
    @PostMapping("/users/{uuid}/role") public Map<String,Object> role(HttpSession session,@PathVariable String uuid,@RequestBody Map<String,Object> body){return service.role(actor(session),uuid,body);}
    @PostMapping("/users/{uuid}/password") public Map<String,Object> reset(HttpSession session,@PathVariable String uuid,@RequestBody Map<String,Object> body){return service.resetPassword(actor(session),uuid,body);}
    @PostMapping("/rescuers/{uuid}/review") public Map<String,Object> review(HttpSession session,@PathVariable String uuid,@RequestBody Map<String,Object> body){return service.review(actor(session),uuid,body);}
    @PostMapping("/urgent/{id}/handle") public Map<String,Object> handle(HttpSession session,@PathVariable long id,@RequestBody Map<String,Object> body){return notifications.handleUrgent(actor(session),id,body);}
    @PostMapping("/notifications") public Map<String,Object> send(HttpSession session,@RequestBody Map<String,Object> body){return notifications.adminSend(actor(session),body);}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/tasks/{id}/assign") public Map<String,Object> assign(HttpSession session,@PathVariable Integer id,@RequestBody Map<String,Object> body){
        String actor=actor(session),reason=BusinessInput.text(body,"reason",2000),rescuer=BusinessInput.text(body,"rescuerUuid",36);
        Map<String,Object> result=tasks.assign(id,rescuer,actor,reason);notifications.audit(actor,"task_assign","task",id,reason);return NotificationService.ok(result);
    }
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/tasks/{id}/unassign") public Map<String,Object> unassign(HttpSession session,@PathVariable Integer id,@RequestBody Map<String,Object> body){
        String actor=actor(session),reason=BusinessInput.text(body,"reason",2000),rescuer=BusinessInput.text(body,"rescuerUuid",36);
        Map<String,Object> result=tasks.unassign(id,rescuer,actor,reason);notifications.audit(actor,"task_unassign","task",id,reason);return NotificationService.ok(result);
    }
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/tasks/{id}/close") public Map<String,Object> close(HttpSession session,@PathVariable Integer id,@RequestBody Map<String,Object> body){
        String actor=actor(session),reason=BusinessInput.text(body,"reason",2000);Map<String,Object> result=tasks.close(id,actor,reason);
        notifications.audit(actor,"task_close","task",id,reason);return NotificationService.ok(result);
    }
    @GetMapping("/content") public Map<String,Object> content(HttpSession session,@RequestParam(defaultValue="")String kind,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit){return content.list(admin(session),kind,status,false,page,limit,true);}
    @PostMapping("/content") public Map<String,Object> saveContent(HttpSession session,@RequestBody Map<String,Object> body){return content.save(admin(session),body,true);}
    @PostMapping("/content/{id}/review") public Map<String,Object> reviewContent(HttpSession session,@PathVariable long id,@RequestBody Map<String,Object> body){return content.review(actor(session),id,body);}
    @DeleteMapping("/content/{id}") public Map<String,Object> deleteContent(HttpSession session,@PathVariable long id){return content.delete(admin(session),id,true);}
    @GetMapping("/system") public Map<String,Object> system(HttpSession session){
        admin(session);jdbc.queryForObject("SELECT 1",Integer.class);
        return NotificationService.ok(Map.of("database","available","notificationChannel","in_app","roles",List.of("family","rescuer","admin"),
                "faceConfigured",configured("BAIDU_FACE_API_KEY"),"voiceConfigured",configured("XFYUN_API_KEY"),"statusNote","仅显示配置状态，不调用外部识别或邮件服务"));
    }
    private boolean configured(String key){String value=environment.getProperty(key);return value!=null&&!value.isBlank();}
}
