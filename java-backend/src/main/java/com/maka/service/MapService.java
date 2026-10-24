package com.maka.service;

import com.maka.pojo.MapMarker;
import java.util.List;
import java.util.Map;

public interface MapService {
    
    /**
     * 获取或创建任务地图配置
     */
    Map<String, Object> getOrCreateMapConfig(Integer taskId);
    
    /**
     * 获取任务的所有地图标记
     */
    List<MapMarker> getMarkersByTaskId(Integer taskId);
    
    /**
     * 获取地图标记详情
     */
    MapMarker getMarkerById(Integer markerId);
    
    /**
     * 保存地图标记
     */
    Integer saveMarker(MapMarker marker);
    
    /**
     * 删除地图标记
     */
    boolean deleteMarker(Integer markerId);

    Map<String, Object> geocodeAddress(String address);


}