package com.maka.mapper;

import com.maka.pojo.SkillTag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SkillTagMapper {
    // 获取所有技能标签
    List<SkillTag> getAllTags();
    
    // 根据名称获取标签
    SkillTag getTagByName(String tagName);
}