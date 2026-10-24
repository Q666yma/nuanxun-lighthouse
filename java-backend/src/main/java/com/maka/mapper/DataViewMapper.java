package com.maka.mapper;

import com.alibaba.fastjson.JSON;
import com.maka.pojo.Rescuer;
import com.maka.pojo.Task;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DataViewMapper {
    List<Integer> selectRescuedTask();
    List<Integer> selectRescuingTask();
    List<String> selectAllRescuer();
    List<List<String>> selectTagList();

}
