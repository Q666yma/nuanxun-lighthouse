package com.maka.support;
import java.util.*;
import com.maka.pojo.TaskMessage;
import com.maka.service.TaskMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class TaskMessageActions {
    @Autowired private TaskAccess access;
    @Autowired private TaskUploads uploads;
    @Autowired private TaskMessageService messages;
    @Autowired private JdbcTemplate db;
    @Transactional(rollbackFor=Exception.class) public TaskMessage save(Integer id,String uid,Map<String,Object> message) throws Exception {
        if(db.queryForList("SELECT id FROM tasks WHERE id=? FOR UPDATE",id).isEmpty() || !access.canWrite(id,uid)) throw new IllegalArgumentException("当前不能向此任务发送消息");
        uploads.bindMessage(message,id,uid);
        return messages.saveMessage(id,new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message));
    }
}
