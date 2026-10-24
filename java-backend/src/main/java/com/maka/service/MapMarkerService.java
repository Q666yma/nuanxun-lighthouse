package com.maka.service;

import com.maka.pojo.MapMarker;
import java.util.List;
import java.util.Map;

public interface MapMarkerService {
    /**
     * 获取任务的所有地图标记
     */
    List<MapMarker> getMarkersByTaskId(Integer taskId);

    /**
     * 保存新的地图标记
     */
    MapMarker saveMarker(MapMarker marker, String userId);

    /**
     * 删除地图标记
     */
    void deleteMarker(Integer markerId, String userId);

    List<Map<String, Object>> generateHeatmapData(Integer taskId);

}