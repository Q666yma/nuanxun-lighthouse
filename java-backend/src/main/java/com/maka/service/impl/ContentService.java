package com.maka.service.impl;

import com.maka.support.BusinessInput;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NotificationService notifications;
    private void validKind(String kind) {if(!Arrays.asList("course","announcement","thanks").contains(kind))NotificationService.reject(HttpStatus.BAD_REQUEST,"内容分类无效");}
    public Map<String,Object> list(Map<String,Object> user,String kind,String status,boolean mine,int page,int limit,boolean admin) {
        if(kind!=null&&!kind.isBlank())validKind(kind);
        page=BusinessInput.page(page);limit=BusinessInput.limit(limit);
        List<Object> args=new ArrayList<>();String where=" WHERE 1=1";
        if(kind!=null&&!kind.isBlank()){where+=" AND c.kind=?";args.add(kind);}
        if(admin) {if(status!=null&&!status.isBlank()){where+=" AND c.status=?";args.add(status);}}
        else if(mine){where+=" AND c.author_uuid=?";args.add(user.get("uuid"));}
        else {where+=" AND c.status='published' AND (c.audience='both' OR c.audience=?)";args.add(user.get("userType"));}
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM site_content c"+where,Integer.class,args.toArray());
        args.add(limit);args.add((page-1)*limit);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT c.id,c.kind,c.title,c.body,c.url,c.category,c.category AS cat,c.duration,'video' AS type,c.audience,c.status,c.author_uuid AS authorUuid,COALESCE(f.name,r.name,p.name,u.username,'系统课程') AS authorName,c.review_reason AS reviewReason,c.created_at AS createdAt,c.updated_at AS updatedAt FROM site_content c LEFT JOIN users u ON u.uuid=c.author_uuid LEFT JOIN families f ON f.uuid=u.uuid LEFT JOIN rescuers r ON r.uuid=u.uuid LEFT JOIN account_profiles p ON p.uuid=u.uuid"+where+" ORDER BY c.id DESC LIMIT ? OFFSET ?",args.toArray());
        return Map.of("success",true,"data",rows,"count",count);
    }
    @Transactional
    public Map<String,Object> save(Map<String,Object> user,Map<String,Object> body,boolean admin) {
        String kind=BusinessInput.text(body,"kind",32),title=BusinessInput.text(body,"title",150);validKind(kind);
        if(!admin&&!"thanks".equals(kind))NotificationService.reject(HttpStatus.FORBIDDEN,"只有管理员可维护课程和公告");
        String text=String.valueOf(body.getOrDefault("body","")),url=String.valueOf(body.getOrDefault("url",""));
        if(text.length()>10000||url.length()>1500)NotificationService.reject(HttpStatus.BAD_REQUEST,"内容过长");
        if("thanks".equals(kind)&&text.isBlank())NotificationService.reject(HttpStatus.BAD_REQUEST,"请填写感谢或反馈内容");
        String audience=admin?String.valueOf(body.getOrDefault("audience","both")):"both";
        if(!Arrays.asList("both","family","rescuer").contains(audience))NotificationService.reject(HttpStatus.BAD_REQUEST,"可见身份无效");
        String status=admin?String.valueOf(body.getOrDefault("status","draft")):"pending";
        if(admin&&!Arrays.asList("draft","published").contains(status))NotificationService.reject(HttpStatus.BAD_REQUEST,"内容状态无效");
        if("course".equals(kind)) {
            if(!(url.startsWith("https://")||(url.startsWith("/")&&!url.startsWith("//")))||url.indexOf('\n')>=0||url.indexOf('\\')>=0)
                NotificationService.reject(HttpStatus.BAD_REQUEST,"课程需填写本站视频路径或HTTPS链接");
        } else url="";
        String category=String.valueOf(body.getOrDefault("category","")),duration=String.valueOf(body.getOrDefault("duration",""));
        if(category.length()>100||duration.length()>32)NotificationService.reject(HttpStatus.BAD_REQUEST,"分类或时长过长");
        String uuid=(String)user.get("uuid");long id;
        if(body.get("id")!=null) {
            id=BusinessInput.id(body.get("id"));
            List<Map<String,Object>> existing=jdbc.queryForList("SELECT id,author_uuid,kind FROM site_content WHERE id=? FOR UPDATE",id);
            if(existing.isEmpty())NotificationService.reject(HttpStatus.NOT_FOUND,"内容不存在");
            if(!admin&&!uuid.equals(existing.get(0).get("author_uuid")))NotificationService.reject(HttpStatus.FORBIDDEN,"只能编辑本人内容");
            if(!admin&&!"thanks".equals(existing.get(0).get("kind")))NotificationService.reject(HttpStatus.FORBIDDEN,"课程和公告只能由管理员维护");
            jdbc.update("UPDATE site_content SET kind=?,title=?,body=?,url=?,category=?,duration=?,audience=?,status=?,reviewed_by=?,review_reason=NULL WHERE id=?",kind,title,text,url,category,duration,audience,status,admin?uuid:null,id);
        } else {
            org.springframework.jdbc.support.GeneratedKeyHolder key=new org.springframework.jdbc.support.GeneratedKeyHolder();
            final String finalUrl=url;
            jdbc.update(connection->{java.sql.PreparedStatement statement=connection.prepareStatement("INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid,reviewed_by) VALUES (?,?,?,?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
                Object[] values={kind,title,text,finalUrl,category,duration,audience,status,uuid,admin?uuid:null};for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},key);
            id=key.getKey().longValue();
        }
        notifications.audit(uuid,admin?"content_save":"content_submit","content",id,title);
        return NotificationService.ok(Map.of("id",id,"status",status));
    }
    @Transactional
    public Map<String,Object> review(String actor,long id,Map<String,Object> body) {
        String status=BusinessInput.text(body,"status",32),reason=BusinessInput.text(body,"reason",2000);
        if(!Arrays.asList("published","rejected").contains(status))NotificationService.reject(HttpStatus.BAD_REQUEST,"审核结果无效");
        if(jdbc.update("UPDATE site_content SET status=?,reviewed_by=?,review_reason=? WHERE id=?",status,actor,reason,id)==0)NotificationService.reject(HttpStatus.NOT_FOUND,"内容不存在");
        notifications.audit(actor,"content_review","content",id,reason);return NotificationService.ok(Map.of("id",id,"status",status));
    }
    @Transactional
    public Map<String,Object> delete(Map<String,Object> user,long id,boolean admin) {
        String uuid=(String)user.get("uuid");
        List<Map<String,Object>> owners=jdbc.queryForList("SELECT author_uuid,kind FROM site_content WHERE id=? FOR UPDATE",id);
        if(owners.isEmpty())NotificationService.reject(HttpStatus.NOT_FOUND,"内容不存在");
        if(!admin&&!uuid.equals(owners.get(0).get("author_uuid")))NotificationService.reject(HttpStatus.FORBIDDEN,"只能删除本人内容");
        if(!admin&&!"thanks".equals(owners.get(0).get("kind")))NotificationService.reject(HttpStatus.FORBIDDEN,"课程和公告只能由管理员维护");
        jdbc.update("DELETE FROM site_content WHERE id=?",id);notifications.audit(uuid,"content_delete","content",id,"删除内容");
        return NotificationService.ok(Map.of("id",id));
    }
}
