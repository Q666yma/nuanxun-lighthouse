package com.maka.service.impl;
import com.maka.pojo.Task;
import com.maka.service.TaskService;
import com.maka.support.TaskAssetUpdates;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FeatureTaskService {
    @Autowired private TaskService tasks;
    @Autowired private TaskAssetUpdates changes;
    @Autowired private JdbcTemplate db;
    @Autowired private TaskFamilyContactService familyContacts;
    public static Integer age(Object raw) {
        if(raw==null||String.valueOf(raw).isBlank())return null;
        String value=String.valueOf(raw);
        if(!value.matches("\\d{1,3}"))throw new IllegalArgumentException("年龄应为0至120岁的整数，未知可留空");
        int age=Integer.parseInt(value);if(age>120)throw new IllegalArgumentException("年龄应为0至120岁的整数，未知可留空");return age;
    }
    @Transactional(rollbackFor=Exception.class)
    public int create(Task task,String uuid,Object rawAge) {
        Integer age=age(rawAge);int id=tasks.createTaskAndAssignToFamily(task,uuid);
        db.update("INSERT INTO task_demographics(task_id,elder_age) VALUES(?,?)",id,age);return id;
    }
    @Transactional(rollbackFor=Exception.class)
    public int create(Task task,String uuid,Object rawAge,Object rawContact) {
        Map<String,Object> contact=familyContacts.prepare(rawContact,uuid);
        int id=create(task,uuid,rawAge);familyContacts.save(id,contact);return id;
    }
    @Transactional(rollbackFor=Exception.class)
    public boolean update(Task task,String uuid,Object rawAge,boolean supplied) {
        Integer age=age(rawAge);boolean ok=changes.update(task,uuid);
        if(ok&&supplied)db.update("INSERT INTO task_demographics(task_id,elder_age) VALUES(?,?) ON DUPLICATE KEY UPDATE elder_age=VALUES(elder_age)",task.getId(),age);
        return ok;
    }
    @Transactional(rollbackFor=Exception.class)
    public boolean update(Task task,String uuid,Object rawAge,boolean supplied,Object rawContact,boolean contactSupplied) {
        Map<String,Object> contact=contactSupplied?familyContacts.prepare(rawContact,uuid):null;
        boolean ok=update(task,uuid,rawAge,supplied);
        if(ok&&contactSupplied)familyContacts.save(task.getId(),contact);
        return ok;
    }
    public Integer getAge(Integer task) {
        List<Integer> values=db.queryForList("SELECT elder_age FROM task_demographics WHERE task_id=?",Integer.class,task);return values.isEmpty()?null:values.get(0);
    }
}
