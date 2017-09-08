package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class Task {
    private String id;
    private String name;
    @JsonProperty("framework_id")
    private String frameworkId;
    @JsonProperty("executor_id")
    private String executorId;
    @JsonProperty("slave_id")
    private String slaveId;
    private String state;
    private List<Label> labels = Collections.emptyList();

    private Resources resources;
    private List<Status> statuses;
}
