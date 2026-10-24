package com.maka.mapper;

import com.maka.pojo.Message;
import com.maka.vo.TimeLine;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface MessageMapper {
    int deleteByPrimaryKey(Integer messageId);

    int insert(Message record);

    int insertSelective(Message record);

    Message selectByPrimaryKey(Integer messageId);

    int updateByPrimaryKeySelective(Message record);

    int updateByPrimaryKey(Message record);

    List<TimeLine> getOldManMessage(@Param("oldManId") int oldManId);
}