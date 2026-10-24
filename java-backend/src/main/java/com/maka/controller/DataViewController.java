package com.maka.controller;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.maka.support.RoleAccess;
import com.maka.service.impl.ScopedDataService;
@RestController @RequestMapping("/data_view")
public class DataViewController {
    @Autowired private RoleAccess roles;
    @Autowired private ScopedDataService data;
    @GetMapping("/data_visualizer") public Map<String,Object> stats(HttpSession session) {
        Map<String,Object> user=roles.current(session);List<Map<String,Object>> tasks=data.tasks(user);
        Map<String,Integer> states=new LinkedHashMap<>(),ages=new LinkedHashMap<>();
        for(String state:List.of("waiting","rescuing","confirming","finished","cancelled"))states.put(state,0);
        for(String age:List.of("0-59","60-69","70-79","80+","unknown"))ages.put(age,0);
        int found=0;
        for(Map<String,Object> task:tasks) {
            states.merge(String.valueOf(task.get("status")),1,Integer::sum);
            if("found".equals(task.get("outcome")))found++;
            Number age=(Number)task.get("elderAge");String band=age==null?"unknown":age.intValue()<60?"0-59":age.intValue()<70?"60-69":age.intValue()<80?"70-79":"80+";
            ages.merge(band,1,Integer::sum);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("RescuedCount",found);result.put("ClosedCount",states.get("finished"));
        result.put("RescuingCount",states.get("waiting")+states.get("rescuing")+states.get("confirming"));
        result.put("TotalTaskNum",tasks.size());result.put("TotalPeopleNum",data.rescuers(user).size());
        result.put("ConfirmingCount",states.get("confirming"));result.put("statusDistribution",states);result.put("ageDistribution",ages);result.put("scope",user.get("userType"));return result;
    }
    @GetMapping("/tag") public Object tags(HttpSession session) {
        List<Map<String,Object>> rows=new ArrayList<>();
        data.tagCounts(roles.current(session)).forEach((name,count)->rows.add(Map.of("name",name,"value",count)));
        return rows;
    }
}
