package com.maka.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 微课程中心页面入口
 */
@Controller
public class CourseCenterController {

    /** 映射 URL:  /rescuer/course_center  */
    @GetMapping("/rescuer/course_center")
    public String courseCenter() {
        // 返回 Thymeleaf 模板的逻辑视图名
        return "rescuer/course_center";
    }
    @GetMapping("/rescuer/thanks_wall")
    public String thanksWall(){
        return "rescuer/thanks_wall";
    }
}
