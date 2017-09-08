package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class State {
    private List<Framework> frameworks = Collections.emptyList();

}
