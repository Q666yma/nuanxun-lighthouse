package com.maka.service.impl;

import com.maka.mapper.FamilyMapper;
import com.maka.pojo.Family;
import com.maka.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 家庭成员服务实现
 */
@Service
public class FamilyServiceImpl implements FamilyService {
    
    @Autowired
    private FamilyMapper familyMapper;
    
    @Override
    public Family getFamilyByUuid(String uuid) {
        return familyMapper.selectByUuid(uuid);
    }
    
    @Override
    public List<Family> getAllFamilies() {
        return familyMapper.selectAll();
    }
    
    @Override
    @Transactional
    public boolean saveOrUpdateFamily(Family family) {
        // 检查是否已存在
        Family existingFamily = familyMapper.selectByUuid(family.getUuid());
        
        if (existingFamily == null) {
            // 不存在则插入
            if (family.getTaskIds() == null) {
                family.setTaskIds(new ArrayList<>());
            }
            return familyMapper.insert(family) > 0;
        } else {
            // 存在则更新
            return familyMapper.update(family) > 0;
        }
    }
    
    @Override
    @Transactional
    public boolean addTaskToFamily(String uuid, Integer taskId) {
        // 检查家庭成员是否存在
        Family family = familyMapper.selectByUuid(uuid);
        if (family == null) {
            return false;
        }
        
        // 检查任务是否已经添加
        if (family.getTaskIds() != null && family.getTaskIds().contains(taskId)) {
            return true; // 任务已存在，视为添加成功
        }
        
        // 添加任务ID
        return familyMapper.addTaskId(uuid, taskId) > 0;
    }
    
    @Override
    @Transactional
    public boolean removeTaskFromFamily(String uuid, Integer taskId) {
        // 检查家庭成员是否存在
        Family family = familyMapper.selectByUuid(uuid);
        if (family == null) {
            return false;
        }
        
        // 检查任务是否存在
        if (family.getTaskIds() == null || !family.getTaskIds().contains(taskId)) {
            return true; // 任务不存在，视为删除成功
        }
        
        // 删除任务ID
        return familyMapper.deleteTaskId(uuid, taskId) > 0;
    }
    
    @Override
    @Transactional
    public boolean deleteFamily(String uuid) {
        return familyMapper.deleteByUuid(uuid) > 0;
    }
    
    @Override
    public boolean isFamily(String uuid) {
        return familyMapper.selectByUuid(uuid) != null;
    }
}