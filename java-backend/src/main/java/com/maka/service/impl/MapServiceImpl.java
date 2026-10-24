package com.maka.service.impl;

import com.maka.mapper.MapMarkerMapper;
import com.maka.mapper.TaskMapper;
import com.maka.pojo.MapMarker;
import com.maka.pojo.Task;
import com.maka.service.MapService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MapServiceImpl implements MapService {
    
    private static final Logger logger = LoggerFactory.getLogger(MapServiceImpl.class);

    @Autowired
    private MapMarkerMapper mapMarkerMapper;
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${amap.key:你的高德地图Key}")
    private String amapKey;

    @Override
    public Map<String, Object> getOrCreateMapConfig(Integer taskId) {
        Map<String, Object> config = new HashMap<>();
        
        // 尝试从任务中获取位置信息
        Task task = taskMapper.selectById(taskId);
        if (task != null) {
            String location = task.getLocation();
            
            try {
                // 首先尝试从位置字符串中提取经纬度
                if (location != null && location.contains("(") && location.contains(")")) {
                    String coords = location.substring(location.indexOf("(") + 1, location.indexOf(")"));
                    String[] latLng = coords.split(",");
                    if (latLng.length == 2) {
                        config.put("centerLat", Double.parseDouble(latLng[0].trim()));
                        config.put("centerLng", Double.parseDouble(latLng[1].trim()));
                        config.put("zoomLevel", 15);
                        return config;
                    }
                }
                
                // 如果没有提取到经纬度，使用地理编码服务
                Map<String, Object> geocodeResult = geocodeAddress(location);
                if (geocodeResult != null) {
                    config.put("centerLat", geocodeResult.get("lat"));
                    config.put("centerLng", geocodeResult.get("lng"));
                    config.put("zoomLevel", 15);
                    config.put("address", location);
                    return config;
                }
            } catch (Exception e) {
                logger.error("解析位置信息失败: " + e.getMessage(), e);
            }
        }
        
        // 如果以上都失败，使用默认配置
        setDefaultMapConfig(config);
        return config;
    }
    
    @Override
    public Map<String, Object> geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        try {
            // 检查是否已编码过。已编码就不要再编码，否则编码一次
            String encodedAddress;
            if (address.contains("%")) {
                encodedAddress = address;
                logger.info("address 已包含 %，不再编码: [{}]", encodedAddress);
            } else {
                encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
                logger.info("address 未编码，编码后: [{}]", encodedAddress);
            }

            String url = "https://restapi.amap.com/v3/geocode/geo?address={address}&key={key}";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, address, amapKey);
            Map<String, Object> data = response.getBody();

            if (data != null && "1".equals(data.get("status"))) {
                List<Map<String, Object>> geocodes = (List<Map<String, Object>>) data.get("geocodes");
                if (geocodes != null && !geocodes.isEmpty()) {
                    Map<String, Object> geocode = geocodes.get(0);
                    String location = (String) geocode.get("location");
                    if (location != null) {
                        String[] lngLat = location.split(",");
                        if (lngLat.length == 2) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("lng", Double.parseDouble(lngLat[0]));
                            result.put("lat", Double.parseDouble(lngLat[1]));
                            return result;
                        }
                    }
                }
            }

            logger.warn("地理编码服务返回无效结果: " + data);
            return null;
        } catch (Exception e) {
            logger.error("调用地理编码服务失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    private void setDefaultMapConfig(Map<String, Object> config) {
        // 默认使用北京中心
        config.put("centerLat", 36.027);
        config.put("centerLng", 120.234);
        config.put("zoomLevel", 15);
    }

    @Override
    public List<MapMarker> getMarkersByTaskId(Integer taskId) {
        return mapMarkerMapper.selectByTaskId(taskId);
    }

    @Override
    public MapMarker getMarkerById(Integer markerId) {
        return mapMarkerMapper.selectById(markerId);
    }

    @Override
    public Integer saveMarker(MapMarker marker) {
        int rows = mapMarkerMapper.insert(marker);
        return rows > 0 ? marker.getId() : null;
    }

    @Override
    public boolean deleteMarker(Integer markerId) {
        int rows = mapMarkerMapper.deleteById(markerId);
        return rows > 0;
    }
}
