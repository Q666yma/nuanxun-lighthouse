package com.maka.service;

import com.maka.pojo.TaskMessage;
import java.util.List;

public interface TaskMessageService {
    /**
     * 保存消息
     * @param taskId 任务ID
     * @param messageData JSON格式的消息数据
     * @return 保存的消息对象
     */
    TaskMessage saveMessage(Integer taskId, String messageData);
    
    /**
     * 获取指定任务的最新消息
     * @param taskId 任务ID
     * @param limit 消息数量限制
     * @return 消息列表
     */
    List<TaskMessage> getMessages(Integer taskId, Integer limit);
    
    /**
     * 获取指定ID之前的消息（向前加载历史消息）
     * @param taskId 任务ID
     * @param messageId 消息ID
     * @param limit 消息数量限制
     * @return 消息列表
     */
    List<TaskMessage> getMessagesBefore(Integer taskId, Integer messageId, Integer limit);
    
    /**
     * 获取指定ID之后的消息（新消息）
     * @param taskId 任务ID
     * @param messageId 消息ID
     * @param limit 消息数量限制
     * @return 消息列表
     */
    List<TaskMessage> getMessagesAfter(Integer taskId, Integer messageId, Integer limit);
    
    /**
     * 获取任务的最新消息ID
     * @param taskId 任务ID
     * @return 最新消息ID，如果没有消息则返回null
     */
    Integer getLatestMessageId(Integer taskId);
}