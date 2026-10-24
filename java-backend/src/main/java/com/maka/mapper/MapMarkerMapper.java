package com.maka.mapper;

import com.maka.pojo.MapMarker;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface MapMarkerMapper {

    List<MapMarker> selectByTaskId(Integer taskId);

    MapMarker selectById(Integer id);

    int insert(MapMarker marker);

    int deleteById(Integer id);

    List<MapMarker> selectAll();

}