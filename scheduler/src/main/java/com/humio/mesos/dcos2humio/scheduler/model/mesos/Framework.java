package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class Framework {
    private String id;
    private String name;
    private String pid;
    private List<Task> tasks = Collections.emptyList();
}
