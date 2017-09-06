package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import lombok.Data;

@Data
public class Resources {
    private double disk;
    private double mem;
    private double gpus;
    private double cpus;
}
