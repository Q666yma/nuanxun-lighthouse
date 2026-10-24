package com.maka.controller;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/show")
public class DummyController {

    @GetMapping("/show-dummy")
    public String dummy(HttpSession session) {
        return "show/show-dummy";  // 对应 resources/templates/dummy.html
    }
} 