package com.maka.service;

import com.maka.pojo.Rescuer;
import com.maka.pojo.SkillTag;
import java.util.List;

public interface RescuerService {
    // 申请成为搜救队员 (创建新记录)
    boolean applyRescuer(Rescuer rescuer);
    
    // 申请或更新搜救队员信息
    boolean updateOrInsertRescuer(Rescuer rescuer);
    
    // 获取当前用户的搜救队员信息
    Rescuer getRescuerByUuid(String uuid);
    
    // 获取所有可用的搜救队员
    List<Rescuer> getAvailableRescuers();
    
    // 更新搜救队员技能标签
    boolean updateRescuerSkillTags(String uuid, List<String> skillTags);
    
    // 从API获取技能标签
    List<String> generateSkillTags(String skillsDescription);
    
    // 获取所有技能标签
    List<SkillTag> getAllSkillTags();

    boolean updateRescuerWithTags(Rescuer rescuer);

    List<Rescuer> getAvailableRescuersPaged(int offset, int limit);
    
    int getAvailableRescuersCount();

    boolean addTaskToRescuer(String uuid, Integer taskId);

}