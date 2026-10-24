package com.maka.mapper;

import com.maka.pojo.Clue;
import com.maka.pojo.OldMan4Two4;
import com.maka.pojo.RescueOldMan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RescueOldManMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(RescueOldMan record);

    int insertSelective(RescueOldMan record);

    RescueOldMan selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(RescueOldMan record);

    int updateByPrimaryKey(RescueOldMan record);

    List<RescueOldMan> selectRescueOldManByPage(@Param("currentPage") int currentPage,@Param("pageSize")  int pageSize);

    int getOneOldManRescueNum(@Param("id") int id);

    Integer getRescueOldManNum();

    List<RescueOldMan> selectPageOldManByCondition(@Param("from")int from, @Param("size")int size, @Param("name") String name,@Param("lostPlace") String lostPlace,@Param("phone") String phone);

    List<Clue> getClueOfOldMan(int oldMan);

    OldMan4Two4 getRandomOldMan();
}