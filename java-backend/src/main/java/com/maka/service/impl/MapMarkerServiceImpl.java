package com.maka.service.impl;

import com.maka.pojo.MapMarker;
import com.maka.pojo.Rescuer;
import com.maka.mapper.MapMarkerMapper;
import com.maka.mapper.RescuerMapper;
import com.maka.service.MapMarkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MapMarkerServiceImpl implements MapMarkerService {

    @Autowired
    private MapMarkerMapper mapMarkerMapper;

    @Autowired private com.maka.support.TaskAccess taskAccess;
    @Autowired private com.maka.mapper.FamilyMapper familyMapper;

    @Autowired
    private RescuerMapper rescuerMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public List<MapMarker> getMarkersByTaskId(Integer taskId) {
        return mapMarkerMapper.selectByTaskId(taskId);
    }

    @Override
    @Transactional
    public MapMarker saveMarker(MapMarker marker, String userId) {
        if (!taskAccess.canWrite(marker.getTaskId(), userId)) throw new IllegalArgumentException("无权限添加标记");
        if (marker.getMarkerType() == null || !java.util.Arrays.asList("marker", "polygon", "polyline", "circle", "rectangle").contains(marker.getMarkerType())) throw new IllegalArgumentException("标记类型无效");
        try {
            com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
            if (marker.getGeometry() == null || marker.getGeometry().length() > 100000 || !json.readTree(marker.getGeometry()).isContainerNode()) throw new IllegalArgumentException("标记坐标无效");
            if (marker.getProperties() != null && !marker.getProperties().isBlank()) {
                if (marker.getProperties().length() > 20000 || !json.readTree(marker.getProperties()).isObject()) throw new IllegalArgumentException("标记属性无效");
            }
        } catch (java.io.IOException ex) { throw new IllegalArgumentException("标记格式无效"); }
        try {
            com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode properties = marker.getProperties() == null || marker.getProperties().isBlank() ? json.createObjectNode() : (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(marker.getProperties());
            com.maka.pojo.Family family = familyMapper.selectByUuid(userId);
            Rescuer rescuer = rescuerMapper.getRescuerByUuid(userId);
            String name = family != null ? family.getName() : (rescuer != null ? rescuer.getName() : "用户");
            properties.put("createdByName", name);
            marker.setProperties(json.writeValueAsString(properties));
        } catch (java.io.IOException ex) { throw new IllegalArgumentException("标记格式无效"); }
        marker.setId(null);
        marker.setCreatedAt(new Date());
        marker.setCreatedBy(userId);
        mapMarkerMapper.insert(marker);
        return marker;
    }

    @Override
    @Transactional
    public void deleteMarker(Integer markerId, String userId) {
        MapMarker marker = mapMarkerMapper.selectById(markerId);
        if (marker == null) {
            throw new RuntimeException("标记不存在");
        }
        if (!userId.equals(marker.getCreatedBy()) || !taskAccess.canWrite(marker.getTaskId(), userId)) throw new IllegalArgumentException("只能删除自己在进行中任务内创建的标记");
        mapMarkerMapper.deleteById(markerId);
    }


    @Value("${ai.heatmap.url:http://localhost:5000/api/mapheat}")
    private String aiHeatmapUrl;

    @Override
    public List<Map<String, Object>> generateHeatmapData(Integer taskId) {
        // 1. 获取所有地图标记
        List<MapMarker> markers = mapMarkerMapper.selectByTaskId(taskId);
        if (markers == null || markers.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 2. 准备数据发送给AI服务
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("taskId", taskId);
        requestData.put("markers", markers);
        
        try {
            // 3. 调用AI服务
            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiHeatmapUrl, 
                localRequest(requestData),
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("heatmapPoints")) {
                // 4. 解析响应
                List<Map<String, Object>> heatmapPoints = (List<Map<String, Object>>) responseBody.get("heatmapPoints");
                System.out.println(heatmapPoints.toString());
                return heatmapPoints;
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw new RuntimeException("热力图生成失败: " + e.getMessage());
        }
    }

    private org.springframework.http.HttpEntity<Map<String,Object>> localRequest(Map<String,Object> data) {
        org.springframework.http.HttpHeaders headers=new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        com.maka.support.RequestCookies.forward(headers);
        return new org.springframework.http.HttpEntity<>(data,headers);
    }
}
