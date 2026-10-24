package com.maka.support;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskUploads {
    @Autowired private JdbcTemplate db;
    @Autowired private TaskAccess access;
    public void register(String path, String owner) {
        db.update("INSERT INTO task_uploads(path,owner_uuid) VALUES (?,?)", path, owner);
    }
    public void requireOwned(String path, String owner) {
        if (path == null || path.isBlank()) return;
        if (!path.matches("/uploads/(img|audio)/[a-zA-Z0-9_-]+\\.[a-zA-Z0-9]+"))
            throw new IllegalArgumentException("请上传本人的照片或音频后再提交");
        if (db.queryForList("SELECT path FROM task_uploads WHERE path=? AND owner_uuid=?", path, owner).isEmpty())
            throw new IllegalArgumentException("只能使用本人上传的资料");
    }
    public void bind(String path, Integer task, String owner) {
        if (path == null || path.isBlank()) return;
        requireOwned(path, owner);
        int changed = db.update("UPDATE task_uploads SET task_id=? WHERE path=? AND owner_uuid=? AND (task_id IS NULL OR task_id=?)", task, path, owner, task);
        if (changed != 1) throw new IllegalArgumentException("该附件已关联其他任务，请重新上传");
    }
    public boolean canRead(String path, String user) {
        List<Map<String,Object>> rows = db.queryForList("SELECT owner_uuid,task_id FROM task_uploads WHERE path=?", path);
        if (rows.isEmpty()) return false;
        Map<String,Object> row = rows.get(0);
        if (user.equals(row.get("owner_uuid"))) return true;
        return row.get("task_id") instanceof Number && access.canView(((Number)row.get("task_id")).intValue(), user, true);
    }
    public void bindMessage(Object value, Integer task, String owner) {
        if (value instanceof String && ((String)value).startsWith("/uploads/")) bind((String)value, task, owner);
        else if (value instanceof Map) for (Object nested : ((Map<?,?>)value).values()) bindMessage(nested, task, owner);
        else if (value instanceof List) for (Object nested : (List<?>)value) bindMessage(nested, task, owner);
    }
}
