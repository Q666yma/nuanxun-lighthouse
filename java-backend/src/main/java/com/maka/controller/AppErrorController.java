package com.maka.controller;


import cn.hutool.Hutool;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.util.RandomUtil;
import com.maka.component.MessageResponse;
import com.maka.pojo.OldMan4Two4;
import com.maka.service.RescueOldManService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@Controller
public class AppErrorController  {
    // @Resource
    // private RescueOldManService rescueOldManService;


    // @ResponseBody
    // @RequestMapping("/test")
    // public MessageResponse test(){
    //    return  MessageResponse.success(rescueOldManService.getRandomOldMan());
    // }

    // @RequestMapping(value = "/404",produces = {"text/html"})
    // public ModelAndView errorPage4xx(){
    //     ModelAndView view = new ModelAndView("/test");
    //     OldMan4Two4 randomOldMan = rescueOldManService.getRandomOldMan();
    //     randomOldMan.setOldGender("("+randomOldMan.getOldGender()+")");
    //     randomOldMan.setFeature(randomOldMan.getIq()+"，"+randomOldMan.getClothing());
    //     view.addObject("oldMan",(Object) randomOldMan);
    //     return view;
    // }

    // @RequestMapping(value = "/500",produces = {"text/html"})
    // public ModelAndView errorPage5xx(){
    //     return new ModelAndView("/test");
    // }


}

