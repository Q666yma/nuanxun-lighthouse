package com.maka.pojo;

import java.io.Serializable;
import java.util.List;

import lombok.Data;


@Data
public class Family implements Serializable {
    private String uuid;
    private String name;
    private List<Integer> taskIds;
    
    // Getters and Setters
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Integer> getTaskIds() {
        return taskIds;
    }
    
    public void setTaskIds(List<Integer> taskIds) {
        this.taskIds = taskIds;
    }
}