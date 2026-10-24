package com.maka.controller;

import com.maka.support.BusinessInput;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** Compatibility URL cannot choose a recipient; the task determines its family. */
@RestController
@RequestMapping("/api/sms")
public class SmsController {
    @Autowired private NotificationController notifications;
    @PostMapping("/send")
    public Map<String,Object> sendSms(HttpSession session,@RequestBody Map<String,Object> body){return notifications.family(session,BusinessInput.id(body.get("taskId")),body);}
}
