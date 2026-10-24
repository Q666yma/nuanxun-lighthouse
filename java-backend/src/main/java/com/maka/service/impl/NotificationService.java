package com.maka.service.impl;

import com.maka.support.RoleAccess;
import com.maka.support.BusinessInput;
import com.maka.service.MailService;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RoleAccess access;
    @Autowired private MailService mail;

    public static Map<String,Object> ok(Object data) { return Map.of("success",true,"data",data); }
    public static void reject(HttpStatus status,String message) { throw new ResponseStatusException(status,message); }

    public Map<String,Object> task(Integer taskId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,elder_name AS elderName,status FROM tasks WHERE id=?",taskId);
        if(rows.isEmpty())reject(HttpStatus.NOT_FOUND,"任务不存在");
        return rows.get(0);
    }
    public boolean owns(Integer taskId,String uuid) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM families WHERE uuid=? AND JSON_CONTAINS(COALESCE(task_ids,JSON_ARRAY()),CAST(? AS JSON))",Integer.class,uuid,String.valueOf(taskId))>0;
    }
    public boolean participates(Integer taskId,String uuid) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM rescuers WHERE uuid=? AND status='available' AND JSON_CONTAINS(COALESCE(task_ids,JSON_ARRAY()),CAST(? AS JSON))",Integer.class,uuid,String.valueOf(taskId))>0;
    }
    public Map<String,Object> authorize(HttpSession session,Integer taskId) {
        Map<String,Object> user=access.current(session);task(taskId);
        String uuid=(String)user.get("uuid"),role=(String)user.get("userType");
        if("admin".equals(role) || ("family".equals(role)&&owns(taskId,uuid))
                ||("rescuer".equals(role)&&Boolean.TRUE.equals(user.get("approved"))&&participates(taskId,uuid)))return user;
        reject(HttpStatus.FORBIDDEN,"只能操作本人或参与的任务");return null;
    }
    private void active(Integer taskId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,elder_name AS elderName,status FROM tasks WHERE id=? FOR UPDATE",taskId);
        if(rows.isEmpty())reject(HttpStatus.NOT_FOUND,"任务不存在");
        if(!Arrays.asList("waiting","rescuing","confirming").contains(rows.get(0).get("status")))
            reject(HttpStatus.CONFLICT,"该任务已结束，不能再发送任务操作通知");
    }
    public List<String> recipients(Integer taskId,String audience) {
        Set<String> result=new LinkedHashSet<>();
        if("family".equals(audience)||"all".equals(audience))result.addAll(jdbc.queryForList(
            "SELECT f.uuid FROM families f JOIN users u ON u.uuid=f.uuid WHERE u.user_type='family' AND u.status='active' AND JSON_CONTAINS(COALESCE(f.task_ids,JSON_ARRAY()),CAST(? AS JSON))",String.class,String.valueOf(taskId)));
        if("rescuers".equals(audience)||"all".equals(audience))result.addAll(jdbc.queryForList(
            "SELECT r.uuid FROM rescuers r JOIN users u ON u.uuid=r.uuid WHERE u.user_type='rescuer' AND u.status='active' AND r.status='available' AND JSON_CONTAINS(COALESCE(r.task_ids,JSON_ARRAY()),CAST(? AS JSON))",String.class,String.valueOf(taskId)));
        return new ArrayList<>(result);
    }
    public void audit(String actor,String action,String targetType,Object target,String reason) {
        jdbc.update("INSERT INTO admin_audit(actor_uuid,action,target_type,target_id,reason) VALUES (?,?,?,?,?)",actor,action,targetType,String.valueOf(target),reason);
    }
    public void send(Integer taskId,String actor,String recipient,String title,String message,String kind,String requestKey) {
        jdbc.update("INSERT IGNORE INTO notifications(task_id,sender_uuid,recipient_uuid,title,message,kind,request_key) VALUES (?,?,?,?,?,?,?)",taskId,actor,recipient,title,message,kind,requestKey);
    }
    /** Called inside task transactions; no network request or external mail. */
    public void taskEvent(Integer taskId,String title,String message) {
        for(String uuid:recipients(taskId,"all"))send(taskId,null,uuid,title,message,"task_event",null);
    }
    public Map<String,Object> list(HttpSession session,int page,int limit,boolean unread) {
        String uuid=(String)access.current(session).get("uuid");page=BusinessInput.page(page);limit=BusinessInput.limit(limit);
        String where=" WHERE recipient_uuid=?"+(unread?" AND read_at IS NULL":"");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,task_id AS taskId,title,message,kind,email_status AS emailStatus,read_at AS readAt,created_at AS createdAt FROM notifications"+where+" ORDER BY id DESC LIMIT ? OFFSET ?",uuid,limit,(page-1)*limit);
        return Map.of("success",true,"data",rows,"count",jdbc.queryForObject("SELECT COUNT(*) FROM notifications"+where,Integer.class,uuid),
            "unreadCount",jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_uuid=? AND read_at IS NULL",Integer.class,uuid));
    }
    public Map<String,Object> read(HttpSession session,long id) {
        String uuid=(String)access.current(session).get("uuid");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE id=? AND recipient_uuid=?",Integer.class,id,uuid)==0)reject(HttpStatus.NOT_FOUND,"消息不存在");
        jdbc.update("UPDATE notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND recipient_uuid=?",id,uuid);
        return ok(Map.of("id",id));
    }
    @Transactional
    public Map<String,Object> urgent(HttpSession session,Integer taskId,Map<String,Object> body) {
        active(taskId);Map<String,Object> user=authorize(session,taskId);
        String uuid=(String)user.get("uuid"),message=BusinessInput.text(body,"message",4000),key=BusinessInput.requestId(body);
        int changed=jdbc.update("INSERT IGNORE INTO urgent_requests(task_id,sender_uuid,message,request_key) VALUES (?,?,?,?)",taskId,uuid,message,key);
        Map<String,Object> request=jdbc.queryForMap("SELECT id,task_id AS taskId,message,status FROM urgent_requests WHERE sender_uuid=? AND request_key=?",uuid,key);
        if(!String.valueOf(taskId).equals(String.valueOf(request.get("taskId")))||!message.equals(request.get("message")))reject(HttpStatus.CONFLICT,"请为新的提交使用新的标识");
        if(changed>0) {
            for(String admin:jdbc.queryForList("SELECT uuid FROM users WHERE user_type='admin' AND status='active'",String.class))
                send(taskId,uuid,admin,"任务紧急求助待处理",message,"urgent","urgent:"+request.get("id"));
            audit(uuid,"urgent_submit","urgent",request.get("id"),message);
        }
        return ok(request);
    }
    public Map<String,Object> urgentList(HttpSession session,Integer taskId) {
        Map<String,Object> user=authorize(session,taskId);
        return ok(jdbc.queryForList("SELECT id,task_id AS taskId,message,status,reply,created_at AS createdAt FROM urgent_requests WHERE task_id=? AND sender_uuid=? ORDER BY id DESC",taskId,user.get("uuid")));
    }
    @Transactional
    public Map<String,Object> handleUrgent(String admin,long id,Map<String,Object> body) {
        String status=BusinessInput.text(body,"status",32),reply=BusinessInput.text(body,"reply",4000);
        if(!Arrays.asList("processing","resolved").contains(status))reject(HttpStatus.BAD_REQUEST,"处理状态无效");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM urgent_requests WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty())reject(HttpStatus.NOT_FOUND,"求助不存在");
        Map<String,Object> request=rows.get(0);
        if("resolved".equals(request.get("status")))reject(HttpStatus.CONFLICT,"该求助已经处理完成");
        jdbc.update("UPDATE urgent_requests SET status=?,reply=?,handled_by=? WHERE id=?",status,reply,admin,id);
        send(((Number)request.get("task_id")).intValue(),admin,(String)request.get("sender_uuid"),"紧急求助处理进展",reply,"urgent_reply",null);
        audit(admin,"urgent_"+status,"urgent",id,reply);
        return ok(Map.of("id",id,"status",status));
    }
    @Transactional
    public Map<String,Object> notifyFamily(HttpSession session,Integer taskId,Map<String,Object> body) {
        active(taskId);Map<String,Object> user=authorize(session,taskId);
        if(!Arrays.asList("rescuer","admin").contains(user.get("userType")))reject(HttpStatus.FORBIDDEN,"家属不能发送此操作，请使用任务沟通");
        String actor=(String)user.get("uuid"),message=BusinessInput.text(body,"message",4000);
        String key="family:"+actor+":"+BusinessInput.requestId(body);
        List<String> family=recipients(taskId,"family");
        if(family.isEmpty())reject(HttpStatus.CONFLICT,"任务没有可接收消息的家属账号");
        for(String uuid:family)send(taskId,actor,uuid,"发现疑似线索，请进一步核实",message,"family_clue",key);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,task_id AS taskId,message,email_status AS emailStatus FROM notifications WHERE sender_uuid=? AND request_key=?",actor,key);
        for(Map<String,Object> row:rows) {
            if(!String.valueOf(taskId).equals(String.valueOf(row.get("taskId")))||!message.equals(row.get("message")))reject(HttpStatus.CONFLICT,"请为新的通知使用新的提交标识");
        }
        // This endpoint intentionally promises only an in-app submission. Email is
        // an explicit separate action after its database transaction commits.
        if(Boolean.TRUE.equals(body.get("email"))) for(Map<String,Object> row:rows)
            jdbc.update("UPDATE notifications SET email_status='pending' WHERE id=? AND email_status='not_requested'",row.get("id"));
        audit(actor,"notify_family","task",taskId,"提交站内线索通知");
        return ok(rows);
    }
    /** Explicit optional mail delivery, invoked after notifyFamily transaction. */
    public String deliverMail(String actor,long id) {
        if(jdbc.update("UPDATE notifications SET email_status='sending' WHERE id=? AND sender_uuid=? AND email_status='pending'",id,actor)==0)
            return jdbc.queryForObject("SELECT email_status FROM notifications WHERE id=? AND sender_uuid=?",String.class,id,actor);
        List<Map<String,Object>> records=jdbc.queryForList("SELECT n.title,n.message,u.phone FROM notifications n JOIN users u ON u.uuid=n.recipient_uuid WHERE n.id=? AND u.status='active'",id);
        if(records.isEmpty()){jdbc.update("UPDATE notifications SET email_status='failed' WHERE id=?",id);return "failed";}
        Map<String,Object> row=records.get(0);
        String email=String.valueOf(row.get("phone")),status="failed";
        if(email.matches("[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]+")) {
            try { mail.sendEmail(email,String.valueOf(row.get("title")),String.valueOf(row.get("message")));status="sent"; }
            catch(Exception ignored) { status="failed"; }
        }
        jdbc.update("UPDATE notifications SET email_status=? WHERE id=?",status,id);
        return status;
    }
    @Transactional
    public Map<String,Object> adminSend(String actor,Map<String,Object> body) {
        Integer taskId=BusinessInput.id(body.get("taskId"));task(taskId);
        String audience=BusinessInput.text(body,"audience",20),title=BusinessInput.text(body,"title",150),message=BusinessInput.text(body,"message",4000);
        if(!Arrays.asList("family","rescuers","all").contains(audience))reject(HttpStatus.BAD_REQUEST,"请选择任务家属或参与队员");
        String key="admin:"+actor+":"+BusinessInput.requestId(body);
        List<Map<String,Object>> existing=jdbc.queryForList("SELECT task_id AS taskId,title,message FROM notifications WHERE sender_uuid=? AND request_key=?",actor,key);
        if(!existing.isEmpty()) {
            Map<String,Object> old=existing.get(0);
            if(!String.valueOf(taskId).equals(String.valueOf(old.get("taskId")))||!title.equals(old.get("title"))||!message.equals(old.get("message")))reject(HttpStatus.CONFLICT,"请为新的通知使用新的提交标识");
            return ok(Map.of("recipientCount",existing.size(),"channel","in_app"));
        }
        List<String> people=recipients(taskId,audience);
        if(people.isEmpty())reject(HttpStatus.CONFLICT,"所选范围没有可接收消息的账号");
        for(String recipient:people)send(taskId,actor,recipient,title,message,"admin_notice",key);
        audit(actor,"targeted_notification","task",taskId,title);
        return ok(Map.of("recipientCount",people.size(),"channel","in_app"));
    }
}
