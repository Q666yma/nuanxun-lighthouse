package com.maka.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.UUID;

/**
 * 用于渲染心理关怀 GPT 对话页面
 */
@Controller
public class PsychCarePageController {

    /**
     * 访问地址：/rescuer/psych_care_chat
     * 返回 Thymeleaf 视图 "rescuer/psych_care_chat"
     */
    @GetMapping("/rescuer/psych_care_chat")
    public String psychCareChat(Model model) {
        // 如果前端脚本里想用 Thymeleaf 变量，可在这里塞进去
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        model.addAttribute("apiUrl", "/api/psych-care/chat");
        return "rescuer/psych_care_chat";   // => templates/rescuer/psych_care_chat.html
    }
}