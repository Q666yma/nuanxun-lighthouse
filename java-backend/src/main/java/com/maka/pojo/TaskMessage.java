package com.maka.pojo;

import java.util.Date;

public class TaskMessage {
    private Integer id;
    private Integer taskId;
    private String messageData;  // JSON字符串
    private Date createdAt;
    
    // 构造函数
    public TaskMessage() {
    }
    
    public TaskMessage(Integer taskId, String messageData) {
        this.taskId = taskId;
        this.messageData = messageData;
    }
    
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
    
    public String getMessageData() {
        return messageData;
    }
    
    public void setMessageData(String messageData) {
        this.messageData = messageData;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "TaskMessage{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", messageData='" + messageData + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}