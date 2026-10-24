package com.maka.controller;

import com.maka.pojo.Rescuer;
import com.maka.pojo.SkillTag;
import com.maka.service.RescuerService;
import com.maka.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

@Controller
@RequestMapping("/rescuer")
public class RescuerController {

    @Autowired
    private RescuerService rescuerService;

    @Autowired
    private UserService userService;
    @Autowired private com.maka.support.RoleAccess roleAccess;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    
    // 跳转到申请页面
    @GetMapping("/apply")
    public String toApplyPage(HttpSession session, Model model) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        if (userId == null) {
            return"common/no-login";
        }else if (!userService.isRescuer(userId)){
            return "common/forbidden-family";
        }
        
        // 检查是否已经申请过
        Rescuer rescuer = rescuerService.getRescuerByUuid(userId);
        if (rescuer != null && !"visitor".equals(rescuer.getStatus())) {
            model.addAttribute("rescuer", rescuer);
            return "rescuer/profile"; // 已存在且状态为available则显示个人资料
        }
        
        // 如果存在但状态不是available，或者不存在，都显示申请表单
        return "rescuer/apply";
    }
    
    // 提交申请
    @PostMapping("/apply")
    @ResponseBody
    public Map<String, Object> applyRescuer(@RequestBody Map<String, String> formData, HttpSession session) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");

        if (userId == null) {
            return Map.of("success", false, "message", "未登录，请重新登录", "code", 401);
        } else if (!userService.isRescuer(userId)) {
            // 对于非救援者，返回权限错误的JSON响应
            return Map.of("success", false, "message", "权限不足，此功能仅对救援人员开放", "code", 403);
        }

        // 创建Rescuer对象并设置属性
        Rescuer rescuer = new Rescuer();
        rescuer.setUuid(userId);
        rescuer.setName(formData.get("name"));
        rescuer.setLocation(formData.get("location"));
        rescuer.setSkillsDescription(formData.get("skillsDescription"));

        // 保存或更新申请信息
        com.maka.support.BusinessInput.text(formData,"name",100);
        boolean result = rescuerService.updateOrInsertRescuer(rescuer);
        if (result) jdbc.update("UPDATE rescuers SET status='pending' WHERE uuid=? AND status IN ('visitor','rejected')",userId);
        
        if (result) {
            return Map.of("success", true, "message", "申请提交成功", "code", 200);
        } else {
            return Map.of("success", false, "message", "申请提交失败", "code", 500);
        }
    }
    
    // 生成技能标签页面
    @GetMapping("/generate-tags")
    public String generateTagsPage(HttpSession session, Model model) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        if (userId == null) {
            return "common/no-login";
        }
        
        Rescuer rescuer = rescuerService.getRescuerByUuid(userId);
        if (rescuer == null) {
            return "redirect:/rescuer/apply";
        }
        
        // 调用服务生成标签
        List<String> generatedTags = rescuerService.generateSkillTags(rescuer.getSkillsDescription());
        
        model.addAttribute("rescuer", rescuer);
        model.addAttribute("generatedTags", generatedTags);
        
        // 获取所有标签
        List<SkillTag> allSkillTags = rescuerService.getAllSkillTags();
        model.addAttribute("allTags", allSkillTags);
        
        return "rescuer/rescuer_tags";
    }
    
    // 保存技能标签
    @PostMapping("/save-tags")
    @ResponseBody
    public Map<String, Object> saveTags(@RequestBody Map<String, Object> params, HttpSession session) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        if (userId == null) {
            return Map.of("success", false, "message", "未登录");
        }

        if (!userService.isRescuer(userId)) return Map.of("success", false, "message", "此功能仅对救援人员开放");
        String name = (String) params.get("name");
        String location = (String) params.get("location");
        String skillsDescription = (String) params.get("skillsDescription");
        @SuppressWarnings("unchecked")
        List<String> skillTags = (List<String>) params.get("tags");


        // 过滤掉无意义的tag
        if (skillTags == null) skillTags = new ArrayList<>();
        skillTags = skillTags.stream()
            .filter(tag -> tag != null && !tag.isBlank() && tag.length() <= 100 && !"未找到匹配标签，请优化个人简介".equals(tag))
            .collect(Collectors.toList());

        // 去重，保留顺序
        skillTags = new ArrayList<>(new LinkedHashSet<>(skillTags));


        // 构造Rescuer对象
        Rescuer rescuer = new Rescuer();
        rescuer.setUuid(userId);
        rescuer.setName(name);
        rescuer.setLocation(location);
        rescuer.setSkillsDescription(skillsDescription);
        rescuer.setSkillTags(skillTags); // 你的Rescuer类需有此属性
        // Review status is server-managed; personal edits never approve an account.

        // 统一调用一个更新方法
        boolean success = rescuerService.updateRescuerWithTags(rescuer);
        if (success) jdbc.update("UPDATE rescuers SET status='pending' WHERE uuid=? AND status='visitor'",userId);

        if (success) {
            return Map.of("success", true, "message", "信息保存成功");
        } else {
            return Map.of("success", false, "message", "保存失败");
        }
    }
    

    @GetMapping("/check-permission")
    @ResponseBody
    public Map<String, Object> checkPermission(HttpSession session) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录");
        }
        
        // 获取当前用户的搜救队员信息
        Rescuer currentRescuer = rescuerService.getRescuerByUuid(userId);
        
        // 只有状态为available的搜救队员才能查看列表
        if (currentRescuer == null || !"available".equals(currentRescuer.getStatus())) {
            return Map.of("code", 403, "msg", "您需要先申请成为搜救队员");
        }
        
        return Map.of("code", 0, "msg", "有权限访问");
    }

    // 获取所有搜救队员列表
    @GetMapping("/list")
    public String getRescuerList(HttpSession session) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        if (userId == null) {
            return "common/no-login";
        }
        
        // 获取当前用户的搜救队员信息
        Rescuer currentRescuer = rescuerService.getRescuerByUuid(userId);
        
        // 只有状态为available的搜救队员才能查看列表
        if (currentRescuer == null || !"available".equals(currentRescuer.getStatus())) {
            return "redirect:/rescuer/apply";
        }
        
        return "redirect:/page/rescuer_list.html";
    }
    @GetMapping("/list-data")
    @ResponseBody
    public Map<String,Object> getRescuerListData(HttpServletRequest request,HttpSession session,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="15") int limit) {
        String uuid=(String)roleAccess.requireApproved(session).get("uuid");
        page=com.maka.support.BusinessInput.page(page);limit=com.maka.support.BusinessInput.limit(limit);
        String scope=" FROM rescuers r JOIN users u ON u.uuid=r.uuid JOIN rescuers me ON me.uuid=? WHERE u.status='active' AND u.user_type='rescuer' AND r.status='available' AND (r.uuid=me.uuid OR JSON_OVERLAPS(COALESCE(r.task_ids,JSON_ARRAY()),COALESCE(me.task_ids,JSON_ARRAY())))";
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT r.uuid,r.name,r.status,r.location,r.skills_description AS skillsDescription,r.skill_tags AS skillTags"+scope+" ORDER BY r.name LIMIT ? OFFSET ?",uuid,limit,(page-1)*limit);
        for(Map<String,Object> row:rows) {
            row.put("skillTags",com.alibaba.fastjson.JSON.parse(String.valueOf(row.get("skillTags"))));
            String target=String.valueOf(row.get("uuid"));
            List<Map<String,Object>> shared=jdbc.queryForList("SELECT t.id,t.elder_name AS elderName,t.status FROM tasks t JOIN rescuers a ON a.uuid=? JOIN rescuers b ON b.uuid=? WHERE JSON_CONTAINS(COALESCE(a.task_ids,JSON_ARRAY()),CAST(t.id AS JSON)) AND JSON_CONTAINS(COALESCE(b.task_ids,JSON_ARRAY()),CAST(t.id AS JSON)) ORDER BY t.id DESC",uuid,target);
            row.put("sharedTasks",shared);
            List<Map<String,Object>> contacts=jdbc.queryForList("SELECT contact_phone AS contactPhone,contact_email AS contactEmail,share_contact AS shareContact FROM account_contacts WHERE uuid=?",target);
            Map<String,Object> contact=contacts.isEmpty()?java.util.Collections.emptyMap():contacts.get(0);
            boolean shares=Boolean.TRUE.equals(contact.get("shareContact"))||"1".equals(String.valueOf(contact.get("shareContact")));
            row.put("shareContact",shares);
            boolean visible=uuid.equals(target)||(shares&&!shared.isEmpty());
            row.put("contactPhone",visible?contact.getOrDefault("contactPhone",""):"");
            row.put("contactEmail",visible?contact.getOrDefault("contactEmail",""):"");
        }
        return Map.of("code",0,"msg","","data",rows,"count",jdbc.queryForObject("SELECT COUNT(*)"+scope,Integer.class,uuid));
    }

        // 个人资料页面
    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        
        if (userId == null) {
            return "common/no-login";
        }
        
        Rescuer rescuer = rescuerService.getRescuerByUuid(userId);
        
        if (rescuer == null) {
            return "redirect:/rescuer/apply";
        }
        
        model.addAttribute("rescuer", rescuer);
        
        return "rescuer/profile";
    }
    @PostMapping("/profile/save")
    public String saveProfile(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        String userId = (String) roleAccess.requireRole(session,"rescuer").get("uuid");
        if (userId == null) {
            return "common/no-login";
        }

        Rescuer rescuer = rescuerService.getRescuerByUuid(userId);
        if (rescuer == null) {
            return "redirect:/rescuer/apply";
        }

        // 更新字段
        rescuer.setName(params.get("name"));
        rescuer.setLocation(params.get("location"));
        rescuer.setSkillsDescription(params.get("skillsDescription"));
        // skillTags 不改，status建议保持原值

        boolean success = rescuerService.updateOrInsertRescuer(rescuer);
        if (success) {
            model.addAttribute("rescuer", rescuerService.getRescuerByUuid(userId));
            model.addAttribute("msg", "保存成功！");
        } else {
            model.addAttribute("rescuer", rescuer);
            model.addAttribute("msg", "保存失败，请重试。");
        }
        return "rescuer/profile";
    }
}
