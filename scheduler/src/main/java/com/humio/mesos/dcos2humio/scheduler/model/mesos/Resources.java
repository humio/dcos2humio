package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Resources {
    private double disk;
    private double mem;
    private double gpus;
    private double cpus;
}
