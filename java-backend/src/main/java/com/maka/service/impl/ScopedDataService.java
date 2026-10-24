package com.maka.service.impl;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;

/** One task scope for dashboard totals, map points and related teams. */
@Service
public class ScopedDataService {
    @Autowired private JdbcTemplate db;
    private String taskScope(Map<String,Object> user,List<Object> args) {
        String role=String.valueOf(user.get("userType"));
        if("admin".equals(role))return "1=1";
        if("rescuer".equals(role)&&!Boolean.TRUE.equals(user.get("approved")))return "1=0";
        args.add(user.get("uuid"));
        String table="family".equals(role)?"families":"rescuers";
        return "EXISTS(SELECT 1 FROM "+table+" owner WHERE owner.uuid=? AND JSON_CONTAINS(COALESCE(owner.task_ids,JSON_ARRAY()),CAST(t.id AS JSON)))";
    }
    public List<Map<String,Object>> tasks(Map<String,Object> user) {
        List<Object> args=new ArrayList<>();String scope=taskScope(user,args);
        return db.queryForList("SELECT t.id,t.elder_name AS elderName,t.lost_time AS lostTime,t.location,t.status,o.outcome,d.elder_age AS elderAge FROM tasks t LEFT JOIN task_outcomes o ON o.task_id=t.id LEFT JOIN task_demographics d ON d.task_id=t.id WHERE "+scope+" ORDER BY t.id DESC",args.toArray());
    }
    public List<Map<String,Object>> rescuers(Map<String,Object> user) {
        List<Object> args=new ArrayList<>();String role=String.valueOf(user.get("userType"));
        String scope="1=1";
        if(!"admin".equals(role)) {
            String own=taskScope(user,args);
            scope="EXISTS(SELECT 1 FROM tasks t WHERE "+own+" AND JSON_CONTAINS(COALESCE(r.task_ids,JSON_ARRAY()),CAST(t.id AS JSON)))";
            if("rescuer".equals(role)&&Boolean.TRUE.equals(user.get("approved"))){scope="("+scope+" OR r.uuid=?)";args.add(user.get("uuid"));}
        }
        return db.queryForList("SELECT r.uuid,r.name,r.location,r.status,r.skill_tags AS skillTags FROM rescuers r JOIN users u ON u.uuid=r.uuid WHERE r.status='available' AND u.status='active' AND u.user_type='rescuer' AND "+scope+" ORDER BY r.name,r.uuid",args.toArray());
    }
    public Map<String,Integer> tagCounts(Map<String,Object> user) {
        Map<String,Integer> counts=new TreeMap<>();
        for(Map<String,Object> person:rescuers(user)) {
            Object raw=person.get("skillTags");
            List<String> tags=raw==null?Collections.emptyList():JSON.parseArray(String.valueOf(raw),String.class);
            if(tags!=null)for(String tag:new LinkedHashSet<>(tags)) if(tag!=null&&!tag.isBlank())counts.merge(tag,1,Integer::sum);
        }
        return counts;
    }
}
