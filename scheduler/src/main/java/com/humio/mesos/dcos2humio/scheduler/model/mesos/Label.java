package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Label {
    private String key;
    private String value;
}
