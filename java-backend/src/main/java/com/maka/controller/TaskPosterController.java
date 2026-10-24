package com.maka.controller;

import com.maka.service.TaskPosterService;
import com.maka.service.UserService;
import com.maka.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/task")
public class TaskPosterController {
    @Autowired private com.maka.support.RoleAccess roles;
    @Autowired private com.maka.support.TaskUploads uploads;
    @Autowired private com.maka.service.TaskService tasks;
    @Autowired private com.maka.support.TaskAccess access;

    @Autowired
    private TaskPosterService posterService;

    @Autowired
    private UserService userService;
    
    /**
     * 生成寻人海报
     */
    @PostMapping("/generate-poster")
    @ResponseBody
    public Map<String, Object> generatePoster(@RequestBody Map<String, Object> requestData, HttpSession session) {
        Map<String,Object> current=roles.requireRole(session,"family","admin");
        Map<String, Object> response = new HashMap<>();
        
        // 检查用户是否已登录
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            response.put("code", 401);
            response.put("msg", "用户未登录");
            return response;
        }
        
        if(requestData.get("taskId")!=null) {
            Integer taskId=com.maka.support.BusinessInput.id(requestData.get("taskId"));
            if(!access.canView(taskId,userId,true))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"无权为此任务生成海报");
            com.maka.pojo.Task task=tasks.getTaskById(taskId);
            requestData=new HashMap<>(requestData);
            requestData.put("elderName",task.getElderName());requestData.put("lostTime",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getLostTime()));
            requestData.put("location",task.getLocation());requestData.put("photoUrl",task.getPhotoUrl());requestData.put("extraInfo",task.getExtraInfo());
        }
        // 基本验证
        String elderName = (String) requestData.get("elderName");
        String lostTime = (String) requestData.get("lostTime");
        String location = (String) requestData.get("location");
        String photoUrl = (String) requestData.get("photoUrl");
        if(requestData.get("taskId")==null)uploads.requireOwned(photoUrl,userId);
        
        if (elderName == null || elderName.trim().isEmpty()) {
            response.put("code", 400);
            response.put("msg", "走失人员姓名不能为空");
            return response;
        }
        
        if (lostTime == null || lostTime.trim().isEmpty()) {
            response.put("code", 400);
            response.put("msg", "走失时间不能为空");
            return response;
        }
        
        if (location == null || location.trim().isEmpty()) {
            response.put("code", 400);
            response.put("msg", "走失地点不能为空");
            return response;
        }
        
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            response.put("code", 400);
            response.put("msg", "走失人员照片不能为空");
            return response;
        }
        
        try {
            // 获取额外信息和模板数据
            String extraInfo = (String) requestData.get("extraInfo");
            @SuppressWarnings("unchecked")
            Map<String, Object> templateData = (Map<String, Object>) requestData.get("templateData");
            
            // A contact telephone is distinct from the account's bound email address.
            String contactPhone=String.valueOf(requestData.getOrDefault("contactPhone",current.getOrDefault("contactPhone",""))).trim();
            if(!contactPhone.isEmpty()&&!contactPhone.matches("[+0-9() -]{3,40}"))throw new IllegalArgumentException("联系电话格式无效");

            // 当前时间
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            // 调用服务生成HTML海报
            Map<String, Object> result = posterService.generateHtmlPoster(
                elderName, lostTime, location, photoUrl, extraInfo, 
                templateData, contactPhone, currentTime, userId);
            
            if ((Boolean) result.get("success")) {
                response.put("code", 0);
                response.put("msg", "海报生成成功");
                response.put("data", result);
            } else {
                response.put("code", 500);
                response.put("msg", result.get("message"));
            }
            
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "服务器错误: " + e.getMessage());
            return response;
        }
    }
    
    
}
