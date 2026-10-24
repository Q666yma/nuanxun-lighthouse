package com.maka.service.impl;

import com.maka.support.BusinessInput;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class AdminService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NotificationService notifications;
    @Autowired private com.maka.support.RoleAccess roles;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
    private int count(String sql,Object...args){return jdbc.queryForObject(sql,Integer.class,args);}
    public Map<String,Object> summary(){return NotificationService.ok(Map.of("pendingRescuers",count("SELECT COUNT(*) FROM rescuers WHERE status='pending'"),"pendingUrgent",count("SELECT COUNT(*) FROM urgent_requests WHERE status IN ('submitted','processing')"),"activeTasks",count("SELECT COUNT(*) FROM tasks WHERE status IN ('waiting','rescuing','confirming')"),"users",count("SELECT COUNT(*) FROM users")));}
    public Map<String,Object> list(String kind,String filter,int page,int limit){
        page=BusinessInput.page(page);limit=BusinessInput.limit(limit);
        String select,from;List<Object> args=new ArrayList<>();
        String names=" LEFT JOIN families f ON f.uuid=u.uuid LEFT JOIN rescuers r ON r.uuid=u.uuid LEFT JOIN account_profiles p ON p.uuid=u.uuid";
        if("users".equals(kind)){select="u.uuid,u.username,COALESCE(f.name,r.name,NULLIF(p.name,''),u.username) AS name,u.user_type AS userType,u.status,u.created_at AS createdAt,r.status AS rescuerStatus";from=" FROM users u"+names;}
        else if("rescuers".equals(kind)){select="r.uuid,u.username,r.name,r.location,r.skills_description AS skillsDescription,r.skill_tags AS skillTags,r.status,u.status AS accountStatus";from=" FROM rescuers r JOIN users u ON u.uuid=r.uuid WHERE u.user_type='rescuer'";if(filter!=null&&!filter.isBlank()){from+=" AND r.status=?";args.add(filter);}}
        else if("tasks".equals(kind)){select="t.id,t.elder_name AS elderName,t.status,t.location,t.lost_time AS lostTime,(SELECT f.name FROM families f WHERE JSON_CONTAINS(COALESCE(f.task_ids,JSON_ARRAY()),CAST(t.id AS JSON)) LIMIT 1) AS familyName";from=" FROM tasks t";}
        else if("urgent".equals(kind)){select="q.id,q.task_id AS taskId,t.elder_name AS taskName,COALESCE(f.name,r.name,NULLIF(p.name,''),u.username) AS senderName,q.message,q.status,q.reply,q.created_at AS createdAt";from=" FROM urgent_requests q JOIN users u ON u.uuid=q.sender_uuid"+names+" LEFT JOIN tasks t ON t.id=q.task_id";if(filter!=null&&!filter.isBlank()){from+=" WHERE q.status=?";args.add(filter);}}
        else if("notifications".equals(kind)){select="n.id,n.task_id AS taskId,n.title,n.message,n.kind,n.email_status AS emailStatus,COALESCE(f.name,r.name,NULLIF(p.name,''),u.username) AS recipientName,n.created_at AS createdAt,n.read_at AS readAt";from=" FROM notifications n JOIN users u ON u.uuid=n.recipient_uuid"+names;}
        else if("audit".equals(kind)){select="a.id,COALESCE(f.name,r.name,NULLIF(p.name,''),u.username) AS actorName,a.action,a.target_type AS targetType,a.target_id AS targetId,a.reason,a.created_at AS createdAt";from=" FROM admin_audit a LEFT JOIN users u ON u.uuid=a.actor_uuid"+names;}
        else {NotificationService.reject(HttpStatus.NOT_FOUND,"管理列表不存在");return null;}
        int total=count("SELECT COUNT(*)"+from,args.toArray());
        String order=Arrays.asList("users","rescuers").contains(kind)?("users".equals(kind)?"u.created_at DESC,u.uuid":"r.status,r.name"):("tasks".equals(kind)?"t.id DESC":("urgent".equals(kind)?"q.id DESC":("audit".equals(kind)?"a.id DESC":"n.id DESC")));
        args.add(limit);args.add((page-1)*limit);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT "+select+from+" ORDER BY "+order+" LIMIT ? OFFSET ?",args.toArray());
        if("rescuers".equals(kind))for(Map<String,Object> row:rows)row.put("skillTags",com.alibaba.fastjson.JSON.parse(String.valueOf(row.get("skillTags"))));
        return Map.of("success",true,"data",rows,"count",total);
    }
    private Map<String,Object> lockUser(String uuid,String actor){
        jdbc.queryForList("SELECT uuid FROM users WHERE user_type='admin' AND status='active' ORDER BY uuid FOR UPDATE");
        // The actor may have been demoted while this transaction waited for the
        // administrator locks. Lock and re-read the real actor before any write.
        jdbc.queryForList("SELECT uuid FROM users WHERE uuid=? FOR UPDATE",actor);
        if(!"admin".equals(roles.currentById(actor).get("userType")))
            NotificationService.reject(HttpStatus.FORBIDDEN,"当前账号已无管理员权限");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT uuid,user_type AS userType,status FROM users WHERE uuid=? FOR UPDATE",uuid);
        if(rows.isEmpty())NotificationService.reject(HttpStatus.NOT_FOUND,"账号不存在");return rows.get(0);
    }
    private void revokeSessions(String uuid){jdbc.update("INSERT INTO account_profiles(uuid,auth_version) VALUES (?,1) ON DUPLICATE KEY UPDATE auth_version=auth_version+1",uuid);}
    @Transactional
    public Map<String,Object> status(String actor,String uuid,Map<String,Object> body){
        String status=BusinessInput.text(body,"status",32),reason=BusinessInput.text(body,"reason",2000);
        if(!Arrays.asList("active","disabled").contains(status))NotificationService.reject(HttpStatus.BAD_REQUEST,"账号状态无效");
        Map<String,Object> user=lockUser(uuid,actor);
        if(actor.equals(uuid)&&"disabled".equals(status))NotificationService.reject(HttpStatus.CONFLICT,"不能停用本人账号");
        if("admin".equals(user.get("userType"))&&"disabled".equals(status)&&count("SELECT COUNT(*) FROM users WHERE user_type='admin' AND status='active'")<=1)NotificationService.reject(HttpStatus.CONFLICT,"必须保留至少一个启用的管理员");
        jdbc.update("UPDATE users SET status=? WHERE uuid=?",status,uuid);revokeSessions(uuid);
        notifications.audit(actor,"account_"+status,"user",uuid,reason);return NotificationService.ok(Map.of("uuid",uuid,"status",status));
    }
    @Transactional
    public Map<String,Object> role(String actor,String uuid,Map<String,Object> body){
        String role=BusinessInput.text(body,"role",32),reason=BusinessInput.text(body,"reason",2000);
        if(!Arrays.asList("family","rescuer","admin").contains(role))NotificationService.reject(HttpStatus.BAD_REQUEST,"身份无效");
        Map<String,Object> user=lockUser(uuid,actor);
        if(actor.equals(uuid))NotificationService.reject(HttpStatus.CONFLICT,"不能修改本人身份");
        if(role.equals(user.get("userType")))return NotificationService.ok(Map.of("uuid",uuid,"userType",role));
        if("admin".equals(user.get("userType"))&&"active".equals(user.get("status"))&&count("SELECT COUNT(*) FROM users WHERE user_type='admin' AND status='active'")<=1)NotificationService.reject(HttpStatus.CONFLICT,"必须保留至少一个启用的管理员");
        if(count("SELECT COUNT(*) FROM families WHERE uuid=? AND JSON_LENGTH(COALESCE(task_ids,JSON_ARRAY()))>0",uuid)>0||count("SELECT COUNT(*) FROM rescuers WHERE uuid=? AND JSON_LENGTH(COALESCE(task_ids,JSON_ARRAY()))>0",uuid)>0)
            NotificationService.reject(HttpStatus.CONFLICT,"此账号已有任务记录，请使用停用或另建身份账号以保留历史归属");
        String name=jdbc.queryForObject("SELECT username FROM users WHERE uuid=?",String.class,uuid);
        if("family".equals(role))jdbc.update("INSERT IGNORE INTO families(uuid,name,task_ids) VALUES (?,?,JSON_ARRAY())",uuid,name);
        if("rescuer".equals(role))jdbc.update("INSERT INTO rescuers(uuid,name,status,task_ids,skill_tags) VALUES (?,?,'visitor',JSON_ARRAY(),JSON_ARRAY()) ON DUPLICATE KEY UPDATE status='visitor'",uuid,name);
        jdbc.update("UPDATE users SET user_type=? WHERE uuid=?",role,uuid);revokeSessions(uuid);
        notifications.audit(actor,"account_role","user",uuid,reason);return NotificationService.ok(Map.of("uuid",uuid,"userType",role));
    }
    @Transactional
    public Map<String,Object> review(String actor,String uuid,Map<String,Object> body){
        String decision=BusinessInput.text(body,"decision",32),reason=BusinessInput.text(body,"reason",2000);
        if(!Arrays.asList("approve","reject").contains(decision))NotificationService.reject(HttpStatus.BAD_REQUEST,"审核决定无效");
        Map<String,Object> user=lockUser(uuid,actor);
        if(!"rescuer".equals(user.get("userType")))NotificationService.reject(HttpStatus.BAD_REQUEST,"此账号不是搜救人员");
        if(!"active".equals(user.get("status")))NotificationService.reject(HttpStatus.CONFLICT,"请先启用账号再审核");
        List<String> state=jdbc.queryForList("SELECT status FROM rescuers WHERE uuid=? FOR UPDATE",String.class,uuid);
        if(state.isEmpty())NotificationService.reject(HttpStatus.NOT_FOUND,"没有申请记录");
        if(!"pending".equals(state.get(0)))NotificationService.reject(HttpStatus.CONFLICT,"仅待审核申请可以执行审核");
        String status="approve".equals(decision)?"available":"rejected";
        jdbc.update("UPDATE rescuers SET status=? WHERE uuid=?",status,uuid);
        notifications.send(null,actor,uuid,"搜救人员申请审核结果",reason,"review",null);
        notifications.audit(actor,"rescuer_"+decision,"rescuer",uuid,reason);return NotificationService.ok(Map.of("uuid",uuid,"status",status));
    }
    @Transactional
    public Map<String,Object> resetPassword(String actor,String uuid,Map<String,Object> body){
        String password=BusinessInput.text(body,"newPassword",72),reason=BusinessInput.text(body,"reason",2000);
        if(password.length()<8||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)NotificationService.reject(HttpStatus.BAD_REQUEST,"密码需8位以上且不超过72字节");
        lockUser(uuid,actor);
        if(actor.equals(uuid))NotificationService.reject(HttpStatus.CONFLICT,"请在个人设置中修改本人密码");
        jdbc.update("UPDATE users SET password=? WHERE uuid=?",passwords.encode(password),uuid);revokeSessions(uuid);
        notifications.audit(actor,"password_reset","user",uuid,reason);return NotificationService.ok(Map.of("uuid",uuid));
    }
}
