package com.maka.mapper;

import com.maka.pojo.TaskMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMessageMapper {
    /**
     * 插入新消息
     * @param message 消息对象
     * @return 影响的行数
     */
    int insert(TaskMessage message);
    
    /**
     * 获取指定任务的最新消息
     * @param taskId 任务ID
     * @param limit 消息数量限制
     * @return 消息列表（按ID降序）
     */
    List<TaskMessage> selectByTaskId(@Param("taskId") Integer taskId, 
                                    @Param("limit") Integer limit);
    
    /**
     * 获取指定ID之前的消息
     * @param taskId 任务ID
     * @param messageId 消息ID
     * @param limit 消息数量限制
     * @return 消息列表（按ID降序）
     */
    List<TaskMessage> selectBeforeId(@Param("taskId") Integer taskId, 
                                   @Param("messageId") Integer messageId,
                                   @Param("limit") Integer limit);
    
    /**
     * 获取指定ID之后的消息
     * @param taskId 任务ID
     * @param messageId 消息ID
     * @param limit 消息数量限制
     * @return 消息列表（按ID升序）
     */
    List<TaskMessage> selectAfterId(@Param("taskId") Integer taskId,
                                  @Param("messageId") Integer messageId,
                                  @Param("limit") Integer limit);
    
    /**
     * 获取任务的最新消息ID
     * @param taskId 任务ID
     * @return 最新消息ID
     */
    Integer selectLatestId(@Param("taskId") Integer taskId);
}