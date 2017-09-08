package com.humio.mesos.dcos2humio.executor;

import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

import java.util.List;

public class FilebeatConfigScope {
    private final String slaveId;
    private final List<TaskDetails> taskDetails;

    public FilebeatConfigScope(String slaveId, List<TaskDetails> taskDetails) {
        this.slaveId = slaveId;
        this.taskDetails = taskDetails;
    }

    public String getSlaveId() {
        return slaveId;
    }

    public List<TaskDetails> getTaskDetails() {
        return taskDetails;
    }
}
