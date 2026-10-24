package com.maka.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@CrossOrigin
@Controller
public class procastController {

    @RequestMapping("/chats")
    public String moveDataView(){
        return "echats.html";
    }



}