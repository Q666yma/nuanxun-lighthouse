package com.maka.service;

import com.maka.pojo.Task;

import java.util.List;
import java.util.Map;

/**
 * 任务业务接口
 */
public interface TaskService {

    /*──────────────────  你已有的方法  ──────────────────*/
    int     createTaskAndAssignToFamily(Task task, String familyUuid);
    Task    getTaskById(Integer id);
    boolean updateTaskStatus(Integer id, String status);
    List<Task> getAllTasks();
    List<Task> getTasksByStatus(String status);
    boolean isTaskBelongsToFamily(Integer taskId, String familyUuid);
    boolean deleteTask(String familyUuid, Integer taskId);
    Map<String,Object> getTasksByFamilyId(String familyUuid,
                                          int page, int limit,
                                          String elderName, String status);
    boolean updateTask(Task task);
    Map<String,Object> getRecommendedRescuersForTask(Integer taskId);

    /*──────────────────  ★新增：志愿者领取任务  ──────────────────*/
    /**
     * 志愿者领取任务<br>
     * 1) tasks.status → rescuing<br>
     * 2) rescuers.task_ids 追加任务号
     *
     * @param taskId      任务 ID
     * @param rescuerUuid 志愿者 UUID
     * @return            true = 成功
     */
    boolean acceptTask(Integer taskId, String rescuerUuid);

    List<Task> getTasksByRescuer(String rescuerUid);
    boolean finishTask(Integer taskId, String rescuerUid);

    Map<String, Object> generateElderInfoSummary(Map<String, Object> templateData);

    // TaskService.java
    int countTasks(String elderName, String location, String status);
    List<Task> selectTasksByPage(int offset, int limit, String elderName, String location, String status);


}
