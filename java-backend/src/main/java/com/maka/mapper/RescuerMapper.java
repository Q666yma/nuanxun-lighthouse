package com.maka.mapper;

import com.maka.pojo.Rescuer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RescuerMapper {
    // 插入新的搜救队员
    int insertRescuer(Rescuer rescuer);
    
    // 更新或插入搜救队员信息
    int updateOrInsertRescuer(Rescuer rescuer);
    
    // 根据UUID查询搜救队员
    Rescuer getRescuerByUuid(String uuid);
    
    // 获取所有可用的搜救队员
    List<Rescuer> getAvailableRescuers();
    
    // 更新搜救队员信息
    int updateRescuer(Rescuer rescuer);
    
    // 更新搜救队员技能标签
    int updateRescuerSkillTags(@Param("uuid") String uuid, @Param("skillTags") List<String> skillTags);

    int updateRescuerWithStatus(Rescuer rescuer);

    List<Rescuer> getAvailableRescuersPaged(@Param("offset") int offset, @Param("limit") int limit);
    int getAvailableRescuersCount();

    int appendTaskId(@Param("uuid") String uuid,@Param("taskId") Integer taskId);

    Rescuer selectByUuid(String uuid);  

}