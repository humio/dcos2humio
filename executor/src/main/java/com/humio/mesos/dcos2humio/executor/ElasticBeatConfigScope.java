package com.humio.mesos.dcos2humio.executor;

import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

import java.util.List;

public class ElasticBeatConfigScope {
    private final String authToken;
    private final String slaveId;
    private final List<TaskDetails> taskDetails;
    private final boolean metricsContainersEnabled;

    public ElasticBeatConfigScope(String authToken, String slaveId, List<TaskDetails> taskDetails, boolean metricsContainersEnabled) {
        this.authToken = authToken;
        this.slaveId = slaveId;
        this.taskDetails = taskDetails;
        this.metricsContainersEnabled = metricsContainersEnabled;
    }

    public String getSlaveId() {
        return slaveId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public List<TaskDetails> getTaskDetails() {
        return taskDetails;
    }

    public boolean isMetricsContainersEnabled() {
        return metricsContainersEnabled;
    }
}
