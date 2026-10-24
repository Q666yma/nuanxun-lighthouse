package com.maka.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maka.service.MapMarkerService;
import com.maka.pojo.MapMarker;
import com.maka.query.ApiResponse;

@RestController
@RequestMapping("/api/map")
public class MapApiController {
    
    @Autowired
    private MapMarkerService mapMarkerService;

    @Autowired private com.maka.support.TaskAccess taskAccess;
    
    /**
     * 获取任务的所有地图标记
     */
    @GetMapping("/markers/{taskId}")
    public ResponseEntity<?> getTaskMarkers(@PathVariable Integer taskId, HttpSession session) {
        if (!taskAccess.canView(taskId, (String) session.getAttribute("userId"), true)) return ResponseEntity.status(403).body(ApiResponse.error("无权限查看此任务地图"));
        List<MapMarker> markers = mapMarkerService.getMarkersByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(markers));
    }
    
    /**
     * 保存新的地图标记
     */
    @PostMapping("/savemarker")
    public ResponseEntity<?> saveMarker(@RequestBody MapMarker markerDTO, HttpSession session) {
        String userId = (String)session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.error("未登录"));
        }
        if (!taskAccess.canWrite(markerDTO.getTaskId(), userId)) return ResponseEntity.status(403).body(ApiResponse.error("仅任务参与者可以在救援进行中添加标记"));
        try {
            MapMarker savedMarker = mapMarkerService.saveMarker(markerDTO, userId);
            return ResponseEntity.ok(ApiResponse.success(savedMarker));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 删除地图标记
    //  */
    // @PostMapping("/marker/delete/{markerId}")
    // public ResponseEntity<?> deleteMarkerPost(@PathVariable Integer markerId, HttpSession session) {
    //     Integer userId = (Integer) session.getAttribute("userId");
    //     if (userId == null) {
    //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "未登录"));
    //     }
        
    //     try {
    //         mapMarkerService.deleteMarker(markerId, userId);
    //         return ResponseEntity.ok(ApiResponse.success(true));
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //             .body(Collections.singletonMap("message", e.getMessage()));
    //     }
    // }

    /**
 * 删除地图标记 (POST方式)
 */
/**
 * 删除地图标记 (POST方式)
 */
    @PostMapping("/marker/delete")
    public ResponseEntity<?> deleteMarkerPost(@RequestParam Integer id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("未登录"));
        }
        
        try {
            mapMarkerService.deleteMarker(id, userId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取AI生成的热力图数据
     */
    @GetMapping("/heatmap/{taskId}")
    public ResponseEntity<?> getHeatmapData(@PathVariable Integer taskId, HttpSession session) {
        if (!taskAccess.canView(taskId, (String) session.getAttribute("userId"), true)) return ResponseEntity.status(403).body(ApiResponse.error("无权限查看此任务地图"));
        try {
            List<Map<String, Object>> heatmapPoints = mapMarkerService.generateHeatmapData(taskId);
            return ResponseEntity.ok(ApiResponse.success(heatmapPoints));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取热力图数据失败: " + e.getMessage()));
        }
    }
} 
