package com.maka.service;

import com.maka.pojo.Family;
import java.util.List;

/**
 * 家庭成员服务接口
 */
public interface FamilyService {
    
    /**
     * 根据UUID获取家庭成员
     * @param uuid 家庭成员UUID
     * @return 家庭成员对象，不存在则返回null
     */
    Family getFamilyByUuid(String uuid);
    
    /**
     * 获取所有家庭成员
     * @return 家庭成员列表
     */
    List<Family> getAllFamilies();
    
    /**
     * 创建或更新家庭成员信息
     * @param family 家庭成员对象
     * @return 操作是否成功
     */
    boolean saveOrUpdateFamily(Family family);
    
    /**
     * 为家庭成员添加任务ID
     * @param uuid 家庭成员UUID
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    boolean addTaskToFamily(String uuid, Integer taskId);
    
    /**
     * 从家庭成员的任务列表中移除任务
     * @param uuid 家庭成员UUID
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    boolean removeTaskFromFamily(String uuid, Integer taskId);
    
    /**
     * 删除家庭成员
     * @param uuid 家庭成员UUID
     * @return 操作是否成功
     */
    boolean deleteFamily(String uuid);
    
    /**
     * 检查用户是否是家庭成员
     * @param uuid 用户UUID
     * @return 是否是家庭成员
     */
    boolean isFamily(String uuid);
}