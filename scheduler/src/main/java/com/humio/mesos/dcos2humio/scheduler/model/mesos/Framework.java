package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Framework {
    private String id;
    private String name;
    private String pid;
    private List<Task> tasks = Collections.emptyList();
    @JsonProperty("completed_tasks")
    private List<Task> completed_tasks = Collections.emptyList();
}
