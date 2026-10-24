package com.maka.pojo;

import java.util.Date;

public class MapMarker {
    private Integer id;
    private Integer taskId;
    private String markerType; // 'polygon', 'polyline', 'marker'
    private String geometry;   // GeoJSON格式的几何数据
    private String properties; // JSON格式的属性(颜色、描述等)
    private String createdBy;  // 创建用户ID
    private Date createdAt;    // 创建时间
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }
    
    public String getMarkerType() {
        return markerType;
    }
    
    public void setMarkerType(String markerType) {
        this.markerType = markerType;
    }
    
    public String getGeometry() {
        return geometry;
    }
    
    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }
    
    public String getProperties() {
        return properties;
    }
    
    public void setProperties(String properties) {
        this.properties = properties;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}