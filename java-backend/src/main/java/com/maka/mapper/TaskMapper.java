package com.maka.mapper;

import com.maka.pojo.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TaskMapper {

    int insert(Task task);
    int update(Task task);

    Task selectById(Integer id);
    List<Task> selectAll();

    List<Task> selectByStatus(@Param("status") String status);
    int    updateStatus(@Param("id") Integer id, @Param("status") String status);

    int deleteById(Integer id);

    List<Task> selectByIdList(@Param("idList") List<Integer> idList);

    int updateStatusToRescuing(@Param("id") Integer id);
    int updateTaskStatus(@Param("id") Integer id, @Param("status") String status);

    int countTasks(@Param("elderName") String elderName,
                   @Param("location")  String location,
                   @Param("status")    String status);

    List<Task> selectTasksByPage(@Param("offset") int offset,
                                 @Param("limit")  int limit,
                                 @Param("elderName") String elderName,
                                 @Param("location")  String location,
                                 @Param("status")    String status);
}
