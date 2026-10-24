package com.maka.mapper;

import com.maka.pojo.Family;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 家庭成员数据访问接口（用XML）
 */
@Mapper
public interface FamilyMapper {
    int insert(Family family);

    int update(Family family);

    Family selectByUuid(String uuid);

    List<Family> selectAll();

    int addTaskId(String uuid, Integer taskId);

    int deleteByUuid(String uuid);

    int deleteTaskId(String uuid, Integer taskId);
}