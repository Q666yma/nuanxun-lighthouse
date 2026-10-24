package com.maka.support;
import com.maka.pojo.Task;
import com.maka.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class TaskAssetUpdates {
    @Autowired private TaskUploads uploads;
    @Autowired private TaskService tasks;
    @Autowired private RoleAccess roles;
    @Transactional public boolean update(Task task,String owner) {
        if(!"family".equals(roles.currentById(owner).get("userType")) || !tasks.isTaskBelongsToFamily(task.getId(),owner)) throw new IllegalArgumentException("只能修改自己的求助");
        uploads.requireOwned(task.getPhotoUrl(),owner);uploads.requireOwned(task.getAudioUrl(),owner);
        if(!tasks.updateTask(task)) throw new IllegalArgumentException("任务已结束或不能修改");
        uploads.bind(task.getPhotoUrl(),task.getId(),owner);uploads.bind(task.getAudioUrl(),task.getId(),owner);
        return true;
    }
}
