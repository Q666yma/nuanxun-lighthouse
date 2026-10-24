package com.maka.controller;

import com.maka.component.MessageResponse;
import com.maka.pojo.OldMan4Two4;
import com.maka.service.RescueOldManService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/gui/jia/")
public class Four0FourController {



    private RescueOldManService rescueOldManService;

    public Four0FourController(RescueOldManService rescueOldManService) {
        this.rescueOldManService = rescueOldManService;
    }


    @RequestMapping(value = "/getOldManInfo",method = RequestMethod.GET)
    public MessageResponse getOldManInfo(){
        OldMan4Two4 randomOldMan = rescueOldManService.getRandomOldMan();
        randomOldMan.setFeature("智力水平："+randomOldMan.getIq()+",走失时的服装："+randomOldMan.getClothing());
        return MessageResponse.success();

    }



}
