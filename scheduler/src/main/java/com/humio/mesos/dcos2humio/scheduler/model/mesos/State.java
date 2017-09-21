package com.humio.mesos.dcos2humio.scheduler.model.mesos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class State {
    private final String id;
    private List<Framework> frameworks = Collections.emptyList();
    @JsonProperty("completed_frameworks")
    private List<Framework> completedFrameworks = Collections.emptyList();
    @JsonProperty("unregistrered_frameworks")
    private List<Framework> unregistreredFrameworks = Collections.emptyList();

}
