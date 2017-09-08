package com.humio.mesos.dcos2humio.scheduler.model;

import com.humio.mesos.dcos2humio.scheduler.model.mesos.Task;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

public class ModelUtils {
    public static TaskDetails.TaskDetailsBuilder from(Task task) {
        return TaskDetails.builder()
                .slaveId(task.getSlaveId())
                .frameworkId(task.getFrameworkId())
                .frameworkName("TODO")
                .taskId(task.getId());
    }
}
