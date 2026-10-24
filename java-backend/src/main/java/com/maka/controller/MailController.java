package com.maka.controller;

import com.maka.service.impl.NotificationService;
import com.maka.support.BusinessInput;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** Compatibility URL now requires a real task and submits an in-app urgent request. */
@RestController
public class MailController {
    @Autowired private NotificationService notifications;
    @PostMapping("/sendNotification")
    public Map<String,Object> sendNotification(HttpSession session,@RequestBody Map<String,Object> body){
        return notifications.urgent(session,BusinessInput.id(body.get("taskId")),body);
    }
}
