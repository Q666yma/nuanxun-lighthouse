package com.maka.support;

import com.maka.mapper.FamilyMapper;
import com.maka.mapper.RescuerMapper;
import com.maka.mapper.TaskMapper;
import com.maka.pojo.Family;
import com.maka.pojo.Rescuer;
import com.maka.pojo.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Shared task access rules for details, messages and map data. */
@Component
public class TaskAccess {
    @Autowired private FamilyMapper familyMapper;
    @Autowired private RescuerMapper rescuerMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private RoleAccess roleAccess;

    public boolean canView(Integer taskId, String userId, boolean participantOnly) {
        if (taskId == null || userId == null) return false;
        Task task = taskMapper.selectById(taskId);
        if (task == null) return false;
        java.util.Map<String,Object> user;
        try { user = roleAccess.currentById(userId); }
        catch (org.springframework.web.server.ResponseStatusException denied) { return false; }
        String role = String.valueOf(user.get("userType"));
        if ("admin".equals(role)) return true;
        Family family = "family".equals(role) ? familyMapper.selectByUuid(userId) : null;
        if (family != null && family.getTaskIds() != null && family.getTaskIds().contains(taskId)) return true;
        Rescuer rescuer = "rescuer".equals(role) ? rescuerMapper.getRescuerByUuid(userId) : null;
        if (rescuer == null || !"available".equals(rescuer.getStatus())) return false;
        if (rescuer.getTaskIds() != null && rescuer.getTaskIds().contains(taskId)) return true;
        return false;
    }

    public boolean canWrite(Integer taskId, String userId) {
        if (!canView(taskId, userId, true)) return false;
        Task task = taskMapper.selectById(taskId);
        return task != null && ("rescuing".equals(task.getStatus()) || "confirming".equals(task.getStatus()));
    }

    public boolean isActive(Integer taskId) {
        Task task = taskId == null ? null : taskMapper.selectById(taskId);
        return task != null && java.util.Set.of("waiting", "rescuing", "confirming").contains(task.getStatus());
    }

    public void requireRecognition(Integer taskId, javax.servlet.http.HttpSession session) {
        java.util.Map<String,Object> user = roleAccess.requireRole(session, "rescuer", "admin");
        if ("rescuer".equals(user.get("userType"))) roleAccess.requireApproved(session);
        if (!canView(taskId, String.valueOf(user.get("uuid")), true))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "只能比对参与或获授权任务的线索");
        if (!isActive(taskId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "任务已结束，不能继续识别");
    }
}
