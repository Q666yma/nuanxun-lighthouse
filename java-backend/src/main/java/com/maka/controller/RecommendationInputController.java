package com.maka.controller;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.maka.service.impl.RecommendationInputService;
@RestController
public class RecommendationInputController {
    @Autowired private RecommendationInputService inputs;
    @GetMapping("/task/recommendation-input/{id}")
    public Map<String,Object> input(@PathVariable Integer id,HttpSession session){return Map.of("code",0,"data",inputs.authorized(id,session));}
}
