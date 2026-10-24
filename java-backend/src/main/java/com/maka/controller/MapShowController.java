package com.maka.controller;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.maka.mapper.MapMarkerMapper;
import com.maka.pojo.*;
import com.maka.service.*;
import com.maka.service.impl.ScopedDataService;
import com.maka.support.*;
@RestController @RequestMapping("/mapshow")
public class MapShowController {
    @Autowired private ScopedDataService data;
    @Autowired private RoleAccess roles;
    @Autowired private MapService maps;
    @Autowired private MapMarkerMapper markerMapper;
    @Autowired private RescuerService rescuers;
    private double[] coordinates(String location) {
        double[] direct=GeoCoordinates.location(location);if(direct!=null)return direct;
        Map<String,Object> geo=maps.geocodeAddress(location);
        if(geo==null||!(geo.get("lng") instanceof Number)||!(geo.get("lat") instanceof Number))return null;
        return GeoCoordinates.valid(((Number)geo.get("lng")).doubleValue(),((Number)geo.get("lat")).doubleValue());
    }
    public double[] getTaskLngLat(Task task){return task==null?null:coordinates(task.getLocation());}
    public double[] getMarkerCenter(MapMarker marker){return marker==null?null:GeoCoordinates.geometry(marker.getGeometry());}
    @GetMapping("/all") public ResponseEntity<?> all(HttpSession session) {
        Map<String,Object> user=roles.current(session);
        List<Map<String,Object>> taskPoints=new ArrayList<>(),markerPoints=new ArrayList<>(),peoplePoints=new ArrayList<>();
        List<Map<String,Object>> tasks=data.tasks(user),people=data.rescuers(user);Set<Integer> ids=new HashSet<>();
        for(Map<String,Object> task:tasks) {
            ids.add(((Number)task.get("id")).intValue());double[] point=coordinates((String)task.get("location"));
            if(point==null)continue;
            Map<String,Object> row=new LinkedHashMap<>(task);row.remove("location");row.put("lng",point[0]);row.put("lat",point[1]);
            Object time=row.get("lostTime");if(time instanceof Date)row.put("lostTime",((Date)time).getTime());taskPoints.add(row);
        }
        for(MapMarker marker:markerMapper.selectAll()) {
            if(!ids.contains(marker.getTaskId()))continue;double[] point=getMarkerCenter(marker);if(point==null)continue;
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",marker.getId());row.put("taskId",marker.getTaskId());row.put("lng",point[0]);row.put("lat",point[1]);row.put("markerType",marker.getMarkerType());row.put("time",marker.getCreatedAt()==null?null:marker.getCreatedAt().getTime());markerPoints.add(row);
        }
        for(Map<String,Object> person:people) {
            double[] point=coordinates((String)person.get("location"));if(point==null)continue;
            peoplePoints.add(Map.of("uuid",person.get("uuid"),"name",Objects.toString(person.get("name"),"搜救人员"),"status",person.get("status"),"lng",point[0],"lat",point[1],"locationSource","profile"));
        }
        return ResponseEntity.ok(Map.of("tasks",taskPoints,"markers",markerPoints,"rescuers",peoplePoints,"scope",user.get("userType"),"unlocatedTasks",tasks.size()-taskPoints.size(),"unlocatedRescuers",people.size()-peoplePoints.size()));
    }
    @GetMapping("/skill-distribution") public ResponseEntity<?> skills(HttpSession session) {
        Map<String,Object> user=roles.current(session);Map<String,Integer> counts=data.tagCounts(user),categories=new LinkedHashMap<>();Map<String,List<Map<String,Object>>> details=new LinkedHashMap<>();
        for(SkillTag tag:rescuers.getAllSkillTags()) {
            int count=counts.getOrDefault(tag.getTagName(),0);if(count==0)continue;
            String category=tag.getTagCategory()==null?"其他技能":tag.getTagCategory();categories.merge(category,count,Integer::sum);
            details.computeIfAbsent(category,k->new ArrayList<>()).add(Map.of("name",tag.getTagName(),"value",count));
        }
        return ResponseEntity.ok(Map.of("categoryCount",categories,"categoryDetails",details,"totalRescuers",data.rescuers(user).size(),"scope",user.get("userType")));
    }
}
