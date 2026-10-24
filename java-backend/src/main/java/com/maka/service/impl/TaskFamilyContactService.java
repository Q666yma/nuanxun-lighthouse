package com.maka.service.impl;

import com.maka.support.RoleAccess;
import com.maka.support.TaskAccess;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Per-task contact snapshots, kept outside the public Task/list/recommendation DTO. */
@Service
public class TaskFamilyContactService {
    @Autowired private JdbcTemplate db;
    @Autowired private RoleAccess roles;
    @Autowired private TaskAccess access;

    public Map<String,Object> prepare(Object raw, String owner) {
        Map<String,Object> account=roles.currentById(owner);
        if(!"family".equals(account.get("userType"))) throw new IllegalArgumentException("只有家属可以填写求助联系人");
        return validate(raw,account.get("email"));
    }

    public static Map<String,Object> validate(Object raw,Object boundEmail) {
        if(!(raw instanceof Map)) throw new IllegalArgumentException("请填写本次求助联系人姓名和与走失人员的关系");
        Map<?,?> values=(Map<?,?>)raw;
        String name=text(values.get("name"),100,"联系人姓名");
        String relationship=text(values.get("relationship"),60,"与走失人员的关系");
        String phone=optionalText(values.get("phone"),40,"联系电话");
        if(!phone.isEmpty()) {
            String digits=phone.replaceAll("[^0-9]","");
            if(!phone.matches("\\+?[0-9() -]+")||digits.length()<6||digits.length()>20)
                throw new IllegalArgumentException("请填写有效联系电话，未知可留空并通过绑定邮箱联系");
        }
        String email=boundEmail instanceof String?((String)boundEmail).trim():"";
        if(email.length()>320||!email.matches("[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]+"))email="";
        if(phone.isEmpty()&&email.isEmpty())throw new IllegalArgumentException("当前账号没有有效绑定邮箱，请填写联系电话以便搜救人员联系");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("name",name);result.put("relationship",relationship);result.put("phone",phone);result.put("email",email);
        return result;
    }

    private static String text(Object raw,int max,String label) {
        String value=optionalText(raw,max,label);
        if(value.isEmpty())throw new IllegalArgumentException(label+"不能为空");
        return value;
    }
    private static String optionalText(Object raw,int max,String label) {
        if(raw!=null&&!(raw instanceof String))throw new IllegalArgumentException(label+"格式无效");
        String value=raw==null?"":((String)raw).trim();
        if(value.length()>max||value.matches("(?s).*\\p{Cntrl}.*"))throw new IllegalArgumentException(label+"过长或包含无效字符");
        return value;
    }

    // Called only inside FeatureTaskService's task create/update transaction.
    public void save(Integer taskId,Map<String,Object> contact) {
        db.update("INSERT INTO task_family_contacts(task_id,contact_name,relationship,contact_phone,bound_email) VALUES(?,?,?,?,?) "+
            "ON DUPLICATE KEY UPDATE contact_name=VALUES(contact_name),relationship=VALUES(relationship),contact_phone=VALUES(contact_phone),bound_email=VALUES(bound_email)",
            taskId,contact.get("name"),contact.get("relationship"),contact.get("phone"),contact.get("email"));
    }

    /** Never return contacts for a task lobby item, even to an approved but unassigned rescuer. */
    public Map<String,Object> visible(Integer taskId,String userId) {
        if(!access.canView(taskId,userId,true))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只能查看本人或参与任务的求助联系人");
        List<Map<String,Object>> rows=db.queryForList(
            "SELECT contact_name AS name,relationship,contact_phone AS phone,bound_email AS email FROM task_family_contacts WHERE task_id=?",taskId);
        return rows.isEmpty()?null:new LinkedHashMap<>(rows.get(0));
    }
}
