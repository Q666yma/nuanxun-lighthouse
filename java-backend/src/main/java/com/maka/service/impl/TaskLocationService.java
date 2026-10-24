package com.maka.service.impl;

import com.maka.support.RoleAccess;
import com.maka.support.TaskAccess;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Location sharing is opt-in, bound to a task, and never inferred from a profile address. */
@Service
public class TaskLocationService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RoleAccess roles;
    @Autowired private TaskAccess access;

    private static void reject(HttpStatus status,String message) { throw new ResponseStatusException(status,message); }
    private String viewer(Integer taskId,HttpSession session) {
        String uid=(String)roles.current(session).get("uuid");
        if(!access.canView(taskId,uid,true)) reject(HttpStatus.FORBIDDEN,"只能查看本人或参与任务的位置记录");
        return uid;
    }
    private String writer(Integer taskId,HttpSession session) {
        if(jdbc.queryForList("SELECT id FROM tasks WHERE id=? FOR UPDATE",taskId).isEmpty())
            reject(HttpStatus.NOT_FOUND,"任务不存在");
        String uid=(String)roles.requireApproved(session).get("uuid");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT u.status,u.user_type,r.status AS rescuerStatus,r.task_ids FROM users u JOIN rescuers r ON r.uuid=u.uuid WHERE u.uuid=? FOR UPDATE",uid);
        if(rows.isEmpty() || !"active".equals(rows.get(0).get("status")) || !"rescuer".equals(rows.get(0).get("user_type")) || !"available".equals(rows.get(0).get("rescuerStatus")))
            reject(HttpStatus.FORBIDDEN,"当前账号不能共享搜救位置");
        if(!access.canWrite(taskId,uid)) reject(HttpStatus.FORBIDDEN,"只能在正在参与的搜救任务中共享位置");
        return uid;
    }
    private static double number(Map<String,Object> body,String key,double min,double max) {
        Object value=body.get(key);
        if(!(value instanceof Number)) {reject(HttpStatus.BAD_REQUEST,"位置参数无效："+key);return 0;}
        double number=((Number)value).doubleValue();
        if(!Double.isFinite(number) || number<min || number>max) reject(HttpStatus.BAD_REQUEST,"位置参数超出范围："+key);
        return number;
    }
    private static Map<String,Object> point(Map<String,Object> row) {
        Map<String,Object> result=new LinkedHashMap<>(row);
        for(String key:List.of("recordedAt","receivedAt")) {
            Object value=result.get(key);
            if(value instanceof java.util.Date) result.put(key,((java.util.Date)value).toInstant().toString());
        }
        result.put("coordinateSystem","wgs84");
        return result;
    }
    // Connector/J exposes DATETIME as LocalDateTime through getObject. Request
    // Timestamp explicitly so the configured JDBC server time zone is honored.
    private static Map<String,Object> locationRow(ResultSet rs,int rowNum) throws SQLException {
        Map<String,Object> row=new ColumnMapRowMapper().mapRow(rs,rowNum);
        for(String key:List.of("recorded_at","received_at","recordedAt","receivedAt"))
            if(row.containsKey(key)) row.put(key,rs.getTimestamp(key));
        return row;
    }
    @Transactional public Map<String,Object> sharing(Integer taskId,Map<String,Object> body,HttpSession session) {
        if(!(body.get("sharing") instanceof Boolean)) reject(HttpStatus.BAD_REQUEST,"请选择是否共享位置");
        boolean enabled=Boolean.TRUE.equals(body.get("sharing"));
        String uid=enabled?writer(taskId,session):(String)roles.current(session).get("uuid");
        if(enabled) jdbc.update("INSERT INTO task_location_sharing(task_id,user_uuid,sharing) VALUES (?,?,1) ON DUPLICATE KEY UPDATE sharing=1,updated_at=CURRENT_TIMESTAMP(3)",taskId,uid);
        else jdbc.update("UPDATE task_location_sharing SET sharing=0 WHERE task_id=? AND user_uuid=?",taskId,uid);
        return NotificationService.ok(Map.of("sharing",enabled));
    }
    @Transactional public Map<String,Object> record(Integer taskId,Map<String,Object> body,HttpSession session) {
        String uid=writer(taskId,session);
        if(!"wgs84".equals(body.get("coordinateSystem"))) reject(HttpStatus.BAD_REQUEST,"请提交浏览器 GPS 原始坐标");
        double lat=number(body,"latitude",-90,90),lng=number(body,"longitude",-180,180),accuracy=number(body,"accuracy",0,100000);
        Instant captured;
        try {captured=Instant.parse(String.valueOf(body.get("recordedAt")));}
        catch(Exception invalid){reject(HttpStatus.BAD_REQUEST,"定位时间无效");return null;}
        Instant now=Instant.now();
        if(captured.isBefore(now.minusSeconds(600)) || captured.isAfter(now.plusSeconds(60))) reject(HttpStatus.BAD_REQUEST,"定位已过期，请重新获取当前位置");
        List<Map<String,Object>> share=jdbc.queryForList("SELECT sharing FROM task_location_sharing WHERE task_id=? AND user_uuid=? FOR UPDATE",taskId,uid);
        if(share.isEmpty() || !Boolean.parseBoolean(String.valueOf(share.get(0).get("sharing"))) && !"1".equals(String.valueOf(share.get(0).get("sharing"))))
            reject(HttpStatus.CONFLICT,"请先主动开启位置共享");
        List<Map<String,Object>> previous=jdbc.query("SELECT id,recorded_at,received_at FROM task_locations WHERE task_id=? AND user_uuid=? ORDER BY id DESC LIMIT 1",TaskLocationService::locationRow,taskId,uid);
        if(!previous.isEmpty()) {
            Map<String,Object> last=previous.get(0);
            if(!captured.isAfter(((Timestamp)last.get("recorded_at")).toInstant()) || ((Timestamp)last.get("received_at")).toInstant().isAfter(now.minusSeconds(5)))
                return NotificationService.ok(Map.of("accepted",false,"reason","等待更新的定位点"));
        }
        jdbc.update("INSERT IGNORE INTO task_locations(task_id,user_uuid,latitude,longitude,accuracy,recorded_at) VALUES (?,?,?,?,?,?)",taskId,uid,lat,lng,accuracy,Timestamp.from(captured));
        return NotificationService.ok(Map.of("accepted",true));
    }
    public Map<String,Object> latest(Integer taskId,HttpSession session) {
        viewer(taskId,session);
        List<Map<String,Object>> rows=jdbc.query("SELECT l.id,l.user_uuid AS userId,COALESCE(r.name,u.username,'搜救人员') AS name,l.latitude,l.longitude,l.accuracy,l.recorded_at AS recordedAt,l.received_at AS receivedAt,CASE WHEN s.sharing=1 AND u.status='active' AND u.user_type='rescuer' AND r.status='available' AND JSON_CONTAINS(COALESCE(r.task_ids,JSON_ARRAY()),CAST(l.task_id AS JSON)) AND t.status IN ('rescuing','confirming') AND l.received_at>=CURRENT_TIMESTAMP-INTERVAL 90 SECOND THEN 1 ELSE 0 END AS live FROM task_locations l JOIN (SELECT user_uuid,MAX(id) AS id FROM task_locations WHERE task_id=? GROUP BY user_uuid) newest ON newest.id=l.id LEFT JOIN users u ON u.uuid=l.user_uuid LEFT JOIN rescuers r ON r.uuid=l.user_uuid JOIN tasks t ON t.id=l.task_id LEFT JOIN task_location_sharing s ON s.task_id=l.task_id AND s.user_uuid=l.user_uuid ORDER BY l.id DESC",TaskLocationService::locationRow,taskId);
        List<Map<String,Object>> data=new ArrayList<>();
        for(Map<String,Object> row:rows) data.add(point(row));
        return NotificationService.ok(data);
    }
    public Map<String,Object> track(Integer taskId,String uid,long beforeId,int limit,HttpSession session) {
        viewer(taskId,session);
        if(uid==null || !uid.matches("[A-Za-z0-9-]{1,36}") || beforeId<0) reject(HttpStatus.BAD_REQUEST,"轨迹查询参数无效");
        limit=Math.max(1,Math.min(limit,1000));
        List<Object> args=new ArrayList<>(List.of(taskId,uid));
        String cursor=beforeId>0?" AND id<?":"";if(beforeId>0)args.add(beforeId);args.add(limit+1);
        List<Map<String,Object>> rows=jdbc.query("SELECT id,user_uuid AS userId,latitude,longitude,accuracy,recorded_at AS recordedAt,received_at AS receivedAt FROM task_locations WHERE task_id=? AND user_uuid=?"+cursor+" ORDER BY id DESC LIMIT ?",TaskLocationService::locationRow,args.toArray());
        boolean more=rows.size()>limit;if(more)rows.remove(rows.size()-1);
        long next=rows.isEmpty()?0:((Number)rows.get(rows.size()-1).get("id")).longValue();
        Collections.reverse(rows);List<Map<String,Object>> data=new ArrayList<>();for(Map<String,Object> row:rows)data.add(point(row));
        return NotificationService.ok(Map.of("points",data,"hasMore",more,"nextBeforeId",next));
    }
}
