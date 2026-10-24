package com.maka.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.maka.service.UserService;

@Controller
public class SceneTrainController {
    
    @Autowired
    UserService userService;

    @GetMapping("/rescuer/scene_train")
    public String sceneTrain(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("userId");

        if (!userService.isRescuer(userId)){
            return "common/forbidden-family";
        }
        // 后端只负责渲染模板，API 交互全走 Flask
        model.addAttribute("apiUrl", "/api/scene-train");
        return "rescuer/scene_train";
    }
}
