package com.humio.mesos.dcos2humio.shared.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class TaskDetails implements Serializable {
    private String type;
    private String slaveId;
    private String frameworkId;
    private String frameworkName;
    private String taskId;

    @Builder.Default
    private String multilinePattern = null;
    @Builder.Default
    private boolean multilineNegate = false;
    @Builder.Default
    private String multilineMatch = "off";
    @Singular
    private List<String> logFiles;
    private String parser;

    public boolean isMultilineEnabled() {
        return !multilineMatch.equalsIgnoreCase("off");
    }

    public List<String> getAbsolutePaths() {
        return logFiles.stream()
                .map(logFile -> "/var/lib/mesos/slave/slaves/" + slaveId + "/frameworks/" + frameworkId + "/executors/" + taskId + "/runs/latest/" + logFile)
                .collect(Collectors.toList());
    }
}
