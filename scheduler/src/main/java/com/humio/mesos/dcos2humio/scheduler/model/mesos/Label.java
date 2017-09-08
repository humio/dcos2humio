package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import lombok.Data;

@Data
public class Label {
    private String key;
    private String value;
}
