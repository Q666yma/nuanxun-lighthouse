package com.maka.controller;

import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.maka.service.TaskService;
import com.maka.service.impl.NotificationService;
import com.maka.support.RoleAccess;
import com.maka.support.AdminTaskActions;

@RestController @RequestMapping("/task")
public class TaskOutcomeController {
    @Autowired private JdbcTemplate db;
    @Autowired private RoleAccess roles;
    @Autowired private TaskService tasks;
    @Autowired private NotificationService notifications;
    private String owned(Integer id,HttpSession session) {
        String uid=String.valueOf(roles.requireRole(session,"family").get("uuid"));
        if (!tasks.isTaskBelongsToFamily(id,uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只能确认自己的求助");
        List<String> status=db.queryForList("SELECT status FROM tasks WHERE id=? FOR UPDATE",String.class,id);
        if(status.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"任务不存在");
        return status.get(0);
    }
    @PostMapping("/confirm-found/{id}") @Transactional
    public Map<String,Object> confirm(@PathVariable Integer id,@RequestBody(required=false) Map<String,Object> body,HttpSession session) {
        String state=owned(id,session);
        if("finished".equals(state)) {
            Integer found=db.queryForObject("SELECT COUNT(*) FROM task_outcomes WHERE task_id=? AND outcome='found'",Integer.class,id);
            if(found!=null&&found>0)return Map.of("code",0,"success",true,"msg","已经确认找到");
            throw new ResponseStatusException(HttpStatus.CONFLICT,"任务已结束，不能改写结案结果");
        }
        if(!Set.of("waiting","rescuing","confirming").contains(state)) throw new ResponseStatusException(HttpStatus.CONFLICT,"任务已撤回");
        db.update("UPDATE tasks SET status='finished' WHERE id=?",id);
        db.update("INSERT INTO task_outcomes(task_id,outcome,recorded_by) VALUES(?,'found',?)",id,session.getAttribute("userId"));
        AdminTaskActions.queueCleanup(db,id);
        notifications.taskEvent(id,"家属已确认找到", "家属确认已找到走失人员，本次求助已结束。");
        return Map.of("code",0,"success",true,"msg","已确认找到，并通知参与人员");
    }
    @PostMapping("/continue/{id}") @Transactional
    public Map<String,Object> continueSearch(@PathVariable Integer id,HttpSession session) {
        String state=owned(id,session);
        if(!"confirming".equals(state)) throw new ResponseStatusException(HttpStatus.CONFLICT,"只有待确认任务可以继续寻找");
        Integer count=db.queryForObject("SELECT COUNT(*) FROM rescuers r JOIN users u ON u.uuid=r.uuid WHERE r.status='available' AND COALESCE(u.status,'active')='active' AND JSON_CONTAINS(COALESCE(r.task_ids,JSON_ARRAY()),CAST(? AS JSON))",Integer.class,id.toString());
        db.update("UPDATE tasks SET status=? WHERE id=?",count!=null&&count>0?"rescuing":"waiting",id);
        notifications.taskEvent(id,"家属反馈需要继续寻找","现有线索尚未确认，本次求助继续进行。");
        return Map.of("code",0,"success",true,"msg","已恢复寻找，并通知参与人员");
    }
}
