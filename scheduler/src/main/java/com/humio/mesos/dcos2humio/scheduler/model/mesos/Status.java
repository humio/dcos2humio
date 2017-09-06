package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import lombok.Data;

@Data
public class Status {
    private String state;
    private double timestamp;
}
