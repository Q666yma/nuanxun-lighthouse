package com.maka.controller;
import com.maka.pojo.Task;
import com.maka.service.*;
import com.maka.support.RoleAccess;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller @RequestMapping("/task")
public class TaskReceiveController {
    private final TaskService taskService;
    private final UserService userService;
    @Autowired private RoleAccess roles;
    @Autowired private JdbcTemplate db;
    @Autowired private TaskMessageService messages;
    public TaskReceiveController(TaskService tasks,UserService users) {taskService=tasks;userService=users;}
    @GetMapping("/task-receive") public String receivePage(HttpSession session) {
        roles.requireApproved(session); return "task/task-management";
    }
    @GetMapping("/task-track") public String trackPage(HttpSession session) {
        roles.requireApproved(session); return "task/task-track";
    }
    @GetMapping("/list-data") @ResponseBody
    public Map<String,Object> getAllTaskList(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer limit,@RequestParam(required=false) String elderName,
            @RequestParam(required=false) String location,@RequestParam(required=false) String status,HttpSession session) {
        roles.requireApproved(session);
        page=Math.max(1,Math.min(1000000,page)); limit=Math.max(1,Math.min(100,limit));
        List<Object> values=new ArrayList<>();
        String where=" WHERE status IN ('waiting','rescuing')";
        if(elderName!=null&&!elderName.isBlank()){where+=" AND elder_name LIKE ?";values.add("%"+elderName+"%");}
        if(location!=null&&!location.isBlank()){where+=" AND location LIKE ?";values.add("%"+location+"%");}
        if(status!=null&&!status.isBlank()){where+=" AND status=?";values.add(status);}
        Integer count=db.queryForObject("SELECT COUNT(*) FROM tasks"+where,Integer.class,values.toArray());
        values.add(limit);values.add((page-1)*limit);
        List<Map<String,Object>> rows=db.queryForList("SELECT id,elder_name AS elderName,lost_time AS lostTime,location,status FROM tasks"+where+" ORDER BY (status='waiting') DESC,id DESC LIMIT ? OFFSET ?",values.toArray());
        for(Map<String,Object> row:rows) {
            String name=String.valueOf(row.getOrDefault("elderName",""));
            row.put("elderName",name.isBlank()?"走失人员":name.substring(0,1)+"**");
            String address=String.valueOf(row.getOrDefault("location",""));
            java.util.regex.Matcher district=java.util.regex.Pattern.compile("^(.{1,24}?[市区县])").matcher(address);
            row.put("location",district.find()?district.group(1):"接单后查看详细地点");
        }
        return Map.of("code",0,"msg","success","count",count==null?0:count,"data",rows);
    }
    @PostMapping("/accept/{id}") @ResponseBody
    public Map<String,Object> accept(@PathVariable Integer id,HttpSession session) {
        String uid=String.valueOf(roles.requireApproved(session).get("uuid"));
        boolean ok=taskService.acceptTask(id,uid);
        return Map.of("code",ok?0:409,"msg",ok?"任务已加入您的列表":"任务当前不能接收");
    }
    @GetMapping("/my-list-data") @ResponseBody
    public Map<String,Object> myListData(HttpSession session,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int limit) {
        String uid=String.valueOf(roles.requireApproved(session).get("uuid"));
        List<Task> rows=taskService.getTasksByRescuer(uid);
        page=Math.max(1,Math.min(1000000,page));limit=Math.max(1,Math.min(100,limit));
        int start=Math.min((page-1)*limit,rows.size());
        return Map.of("code",0,"msg","success","count",rows.size(),"data",rows.subList(start,Math.min(start+limit,rows.size())));
    }
    @PostMapping("/finish/{id}") @ResponseBody
    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public Map<String,Object> finish(@PathVariable Integer id,@RequestBody(required=false) Map<String,Object> body,HttpSession session) throws Exception {
        Map<String,Object> user=roles.requireApproved(session);
        String uid=String.valueOf(user.get("uuid"));
        String note=body==null?"":String.valueOf(body.getOrDefault("message",""));
        if(note.length()>2000) throw new IllegalArgumentException("行动结果最多2000字");
        Task before=taskService.getTaskById(id);
        String state=before==null?"":before.getStatus();
        boolean ok=taskService.finishTask(id,uid);
        if(ok&&!"confirming".equals(state)&&!note.isBlank()) {
            Map<String,Object> content=new LinkedHashMap<>();
            content.put("type","text");content.put("content","行动结果（待核实）："+note);
            content.put("sender",Map.of("id",uid,"name",String.valueOf(user.getOrDefault("name","搜救人员")),"type","rescuer"));
            messages.saveMessage(id,new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(content));
        }
        return Map.of("code",ok?0:409,"msg",ok?"已提交行动结果，等待家属核实确认":"只能提交自己正在参与的任务结果");
    }
}
