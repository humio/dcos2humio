package com.humio.mesos.dcos2humio.scheduler.model;

import com.humio.mesos.dcos2humio.scheduler.model.mesos.Label;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.Task;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

import java.util.Map;
import java.util.stream.Collectors;

public class ModelUtils {
    public static TaskDetails.TaskDetailsBuilder from(Task task) {
        final TaskDetails.TaskDetailsBuilder taskDetailsBuilder = TaskDetails.builder()
                .slaveId(task.getSlaveId())
                .frameworkId(task.getFrameworkId())
                .taskId(task.getId());
        Map<String, String> humioLabels = task.getLabels().stream().filter(label -> label.getKey().startsWith("HUMIO_")).collect(Collectors.toMap(Label::getKey, Label::getValue));
        if (humioLabels.containsKey("HUMIO_MULTILINE_PATTERN")) {
            taskDetailsBuilder
                    .multilinePattern(humioLabels.get("HUMIO_MULTILINE_PATTERN"))
                    .multilineNegate(Boolean.parseBoolean(humioLabels.get("HUMIO_MULTILINE_NEGATE")))
                    .multilineMatch(humioLabels.get("HUMIO_MULTILINE_MATCH"));
        }
        return taskDetailsBuilder;
    }
}
