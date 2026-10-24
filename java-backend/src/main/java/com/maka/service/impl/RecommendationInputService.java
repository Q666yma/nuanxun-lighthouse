package com.maka.service.impl;

import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.maka.mapper.TaskMapper;
import com.maka.pojo.Task;
import com.maka.support.RoleAccess;

@Service
public class RecommendationInputService {
    @Autowired private JdbcTemplate db;
    @Autowired private RoleAccess roles;
    @Autowired private TaskMapper tasks;
    public Map<String,Object> authorized(Integer id,HttpSession session) {
        Map<String,Object> user=roles.requireRole(session,"family","admin");
        if(!"admin".equals(user.get("userType"))&&db.queryForObject("SELECT COUNT(*) FROM families WHERE uuid=? AND JSON_CONTAINS(COALESCE(task_ids,JSON_ARRAY()),CAST(? AS JSON))",Integer.class,user.get("uuid"),String.valueOf(id))==0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只能为自己的任务推荐人员");
        return build(id);
    }
    public Map<String,Object> build(Integer id) {
        Task task=tasks.selectById(id);
        if(task==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"任务不存在");
        if(!Set.of("waiting","rescuing","confirming").contains(task.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"任务已结束，无需继续调度");
        Map<String,Object> detail=new LinkedHashMap<>();detail.put("id",task.getId());detail.put("elderName",task.getElderName());detail.put("location",task.getLocation());detail.put("extraInfo",task.getExtraInfo());
        List<Map<String,Object>> people=db.queryForList("SELECT r.uuid,r.name,r.location,r.skill_tags AS skillTags,(SELECT COUNT(*) FROM tasks t JOIN task_outcomes o ON o.task_id=t.id AND o.outcome='found' WHERE t.status='finished' AND JSON_CONTAINS(COALESCE(r.task_ids,JSON_ARRAY()),CAST(t.id AS JSON))) AS successfulTasks FROM rescuers r JOIN users u ON u.uuid=r.uuid WHERE r.status='available' AND u.status='active' AND u.user_type='rescuer' ORDER BY r.uuid");
        for(Map<String,Object> person:people) {
            Object raw=person.get("skillTags");person.put("skillTags",raw==null?Collections.emptyList():com.alibaba.fastjson.JSON.parseArray(String.valueOf(raw),String.class));
        }
        return Map.of("task",detail,"rescuers",people,"tagLibrary",db.queryForList("SELECT tag_name FROM skill_tags ORDER BY id",String.class));
    }
}
