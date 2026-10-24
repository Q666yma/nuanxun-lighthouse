package com.maka.support;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.maka.service.TaskService;
import com.maka.service.impl.NotificationService;

@Component
public class AdminTaskActions {
    @Autowired private JdbcTemplate db;
    @Autowired private TaskService tasks;
    @Autowired private RoleAccess roles;
    @Autowired private NotificationService notifications;
    private void admin(String uid,String reason) {
        if (!"admin".equals(roles.currentById(uid).get("userType"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (reason == null || reason.isBlank() || reason.length()>1000) throw new IllegalArgumentException("请填写操作原因（最多1000字）");
    }
    @Transactional public Map<String,Object> assign(Integer taskId,String rescuerUuid,String adminUuid,String reason) {
        admin(adminUuid,reason);
        Map<String,Object> target=roles.currentById(rescuerUuid);
        if (!"rescuer".equals(target.get("userType")) || !"available".equals(target.get("rescuerStatus")))
            throw new IllegalArgumentException("只能指派已审核且未停用的搜救者");
        if (!tasks.acceptTask(taskId,rescuerUuid)) throw new IllegalArgumentException("任务当前不能指派");
        notifications.taskEvent(taskId,"任务人员已调整", "管理员已安排参与人员。"+reason);
        return Map.of("success",true,"message","已指派","taskId",taskId);
    }
    @Transactional public Map<String,Object> unassign(Integer taskId,String rescuerUuid,String adminUuid,String reason) {
        admin(adminUuid,reason);
        List<Map<String,Object>> rows=db.queryForList("SELECT status FROM tasks WHERE id=? FOR UPDATE",taskId);
        if (rows.isEmpty() || !Set.of("waiting","rescuing","confirming").contains(rows.get(0).get("status"))) throw new IllegalArgumentException("只能调整未结束的任务");
        // Inform the removed member before revoking access to the task.
        notifications.taskEvent(taskId,"任务人员调整", "管理员调整了参与人员，请查看当前任务列表。"+reason);
        db.update("UPDATE rescuers SET task_ids=(SELECT IF(COUNT(*)=0,JSON_ARRAY(),JSON_ARRAYAGG(item)) FROM JSON_TABLE(COALESCE(task_ids,JSON_ARRAY()),'$[*]' COLUMNS(item INT PATH '$')) j WHERE item<>?) WHERE uuid=?",taskId,rescuerUuid);
        Integer count=db.queryForObject("SELECT COUNT(*) FROM rescuers r JOIN users u ON u.uuid=r.uuid WHERE r.status='available' AND u.status='active' AND JSON_CONTAINS(COALESCE(r.task_ids,JSON_ARRAY()),CAST(? AS JSON))",Integer.class,taskId.toString());
        if (count!=null && count==0 && "rescuing".equals(rows.get(0).get("status"))) db.update("UPDATE tasks SET status='waiting' WHERE id=?",taskId);
        return Map.of("success",true,"message","已调整参与人员");
    }
    @Transactional public Map<String,Object> close(Integer taskId,String adminUuid,String reason) {
        admin(adminUuid,reason);
        List<Map<String,Object>> rows=db.queryForList("SELECT status FROM tasks WHERE id=? FOR UPDATE",taskId);
        if (rows.isEmpty()) throw new IllegalArgumentException("任务不存在");
        if (Set.of("finished","cancelled").contains(rows.get(0).get("status"))) return Map.of("success",true,"message","任务已经结束");
        db.update("UPDATE tasks SET status='finished' WHERE id=?",taskId);
        db.update("INSERT INTO task_outcomes(task_id,outcome,recorded_by) VALUES(?,'closed',?)",taskId,adminUuid);
        queueCleanup(db,taskId);
        notifications.taskEvent(taskId,"任务已由管理员结束",reason);
        return Map.of("success",true,"message","任务已结束");
    }
    public static void queueCleanup(JdbcTemplate db,Integer id) {
        db.update("INSERT INTO face_cleanup_queue(task_id) VALUES(?) ON DUPLICATE KEY UPDATE task_id=VALUES(task_id)",id);
        db.update("INSERT INTO voice_cleanup_queue(task_id,group_id) SELECT task_id,group_id FROM task_voice_features WHERE task_id=? ON DUPLICATE KEY UPDATE group_id=VALUES(group_id)",id);
    }
}
