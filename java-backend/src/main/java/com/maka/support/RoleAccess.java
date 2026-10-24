package com.maka.support;

import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** The database, never a client field or a stale session role, is authoritative. */
@Component
public class RoleAccess {
    @Autowired private JdbcTemplate jdbc;

    public Map<String,Object> current(HttpSession session) {
        Object id = session == null ? null : session.getAttribute("userId");
        if (!(id instanceof String)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        Map<String,Object> user = currentById((String) id);
        long version=((Number)user.get("authVersion")).longValue();
        Object sessionVersion=session.getAttribute("authVersion");
        if ((sessionVersion instanceof Number ? ((Number)sessionVersion).longValue() : 0L)!=version) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"账号安全信息已更新，请重新登录");
        }
        session.setAttribute("userType", user.get("userType"));
        return user;
    }
    public Map<String,Object> currentById(String uuid) {
        if (uuid == null || uuid.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT u.uuid,u.username,u.user_type AS userType,u.status,u.phone AS email,u.phone,u.gender,u.created_at AS createdAt," +
            "COALESCE(f.name,r.name,NULLIF(p.name,''),u.username) AS name,COALESCE(r.location,p.location,'') AS location,r.status AS rescuerStatus,COALESCE(p.auth_version,0) AS authVersion," +
            "COALESCE(c.contact_phone,'') AS contactPhone,COALESCE(c.contact_email,'') AS contactEmail,COALESCE(c.share_contact,0) AS shareContact " +
            "FROM users u LEFT JOIN account_profiles p ON p.uuid=u.uuid LEFT JOIN account_contacts c ON c.uuid=u.uuid LEFT JOIN families f ON f.uuid=u.uuid LEFT JOIN rescuers r ON r.uuid=u.uuid WHERE u.uuid=?", uuid);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不存在，请重新登录");
        Map<String,Object> user = new LinkedHashMap<>(rows.get(0));
        if (!"active".equals(user.get("status"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已停用，请联系管理员");
        if (!Arrays.asList("family","rescuer","admin").contains(user.get("userType")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号身份无效，请联系管理员");
        user.put("approved", "rescuer".equals(user.get("userType")) && "available".equals(user.get("rescuerStatus")));
        user.put("shareContact",Boolean.TRUE.equals(user.get("shareContact")) || "1".equals(String.valueOf(user.get("shareContact"))));
        return user;
    }
    public String role(HttpSession session) { return (String)current(session).get("userType"); }
    public Map<String,Object> requireRole(HttpSession session, String... roles) {
        Map<String,Object> user = current(session);
        if (!Arrays.asList(roles).contains(user.get("userType"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前身份无权使用此功能");
        return user;
    }
    public Map<String,Object> requireApproved(HttpSession session) {
        Map<String,Object> user = requireRole(session, "rescuer");
        if (!Boolean.TRUE.equals(user.get("approved"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请等待搜救人员审核通过后再执行任务");
        return user;
    }
}
