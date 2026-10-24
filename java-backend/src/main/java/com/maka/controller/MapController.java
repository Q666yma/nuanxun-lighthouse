package com.maka.controller;
import javax.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.maka.support.*;
import com.maka.service.*;
@Controller @RequestMapping("/task")
public class MapController {
    @Autowired private TaskAccess access;
    @Autowired private RoleAccess roles;
    @Autowired private TaskService tasks;
    @Autowired private MapService maps;
    @GetMapping("/task-map/{taskId}")
    public String showTaskMap(@PathVariable Integer taskId,Model model,HttpSession session) {
        Map<String,Object> user=roles.current(session);
        if(!access.canView(taskId,String.valueOf(user.get("uuid")),true)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"无权查看此任务地图");
        model.addAttribute("task",tasks.getTaskById(taskId));
        model.addAttribute("taskId",taskId);
        model.addAttribute("userType",user.get("userType"));model.addAttribute("userName",user.get("name"));
        model.addAttribute("mapConfig",maps.getOrCreateMapConfig(taskId));
        return "task/task-map";
    }
}
