package com.maka.service.impl;

import com.maka.mapper.TaskMessageMapper;
import com.maka.pojo.TaskMessage;
import com.maka.service.TaskMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskMessageServiceImpl implements TaskMessageService {
    
    @Autowired
    private TaskMessageMapper taskMessageMapper;
    
    @Override
    @Transactional
    public TaskMessage saveMessage(Integer taskId, String messageData) {
        TaskMessage message = new TaskMessage(taskId, messageData);
        taskMessageMapper.insert(message);
        return message;
    }
    
    @Override
    public List<TaskMessage> getMessages(Integer taskId, Integer limit) {
        return taskMessageMapper.selectByTaskId(taskId, limit);
    }
    
    @Override
    public List<TaskMessage> getMessagesBefore(Integer taskId, Integer messageId, Integer limit) {
        return taskMessageMapper.selectBeforeId(taskId, messageId, limit);
    }
    
    @Override
    public List<TaskMessage> getMessagesAfter(Integer taskId, Integer messageId, Integer limit) {
        return taskMessageMapper.selectAfterId(taskId, messageId, limit);
    }
    
    @Override
    public Integer getLatestMessageId(Integer taskId) {
        return taskMessageMapper.selectLatestId(taskId);
    }
}