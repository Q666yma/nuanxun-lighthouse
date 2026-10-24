package com.maka.controller;

import com.maka.component.MessageResponse;
import com.maka.pojo.Admin;
import com.maka.utils.JwtUtil;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class LoginController {

    // @PostMapping("/login")
    // public MessageResponse login(Admin admin){

    //     String token = JwtUtil.createToken(admin.getAdminName(), JwtUtil.TTL, JwtUtil.SECRET);
    //     if(token!=null){
    //         return MessageResponse.success(token);
    //     }
    //     return MessageResponse.userError("认证失败");


    // }



}
