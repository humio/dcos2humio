package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import lombok.Data;

import java.util.List;

@Data
public class Tasks {
    private List<Task> tasks;

}
