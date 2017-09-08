package com.humio.mesos.dcos2humio.scheduler.service;

import com.containersolutions.mesos.scheduler.UniversalScheduler;
import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.humio.mesos.dcos2humio.scheduler.model.ModelUtils;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.Label;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.State;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilebeatConfigurationGenerator {
    private static final Logger logger = LoggerFactory.getLogger(FilebeatConfigurationGenerator.class);

    private final MesosConfigProperties mesosConfigProperties;
    private final RestTemplate restTemplate;
    private final UniversalScheduler universalScheduler;

    public FilebeatConfigurationGenerator(MesosConfigProperties mesosConfigProperties, RestTemplateBuilder restTemplateBuilder, UniversalScheduler universalScheduler) {
        this.mesosConfigProperties = mesosConfigProperties;
        this.universalScheduler = universalScheduler;
        this.restTemplate = restTemplateBuilder.rootUri("http://" + mesosConfigProperties.getMaster()).build();
    }

    @Scheduled(fixedDelay = 10000L)
    public void updateConfig() {
        logger.info("Updating Filebeat configuration");
        final ResponseEntity<State> stateEntity = restTemplate.getForEntity("/state", State.class);
        if (stateEntity.getStatusCode().isError()) {
            throw new RuntimeException("Failed to fetch Mesos state: " + stateEntity.getStatusCode().getReasonPhrase());
        }
        if (stateEntity.getBody() == null) {
            throw new RuntimeException("Mesos state responded with empty body");
        }
        stateEntity.getBody().getFrameworks().stream()
                .flatMap(framework -> framework.getTasks().stream())
                .filter(task -> task.getState().equals("TASK_RUNNING")) //TODO: included all tasks that are not older than one day
                .filter(task -> task.getLabels().stream().filter(label -> "HUMIO_IGNORE" .equals(label.getKey())).map(Label::getValue).noneMatch(Boolean::parseBoolean))
                .map(task -> ModelUtils.from(task).logFile("stdout").logFile("stderr").build())
                .collect(Collectors.groupingBy(TaskDetails::getSlaveId))
                .forEach((slaveId, taskDetails) -> {
                    logger.info("Updating config on {}", slaveId);
                    pushConfig(slaveId, taskDetails);
                });
    }

    private void pushConfig(String slaveId, List<TaskDetails> taskDetails) {
//        final byte[] data = taskDetails.stream().map(TaskDetails::toString).collect(Collectors.joining("\n")).getBytes();
        final byte[] data = SerializationUtils.serialize(((Serializable) taskDetails));

        universalScheduler.sendFrameworkMessage("humioexecutor." + slaveId, slaveId, data);
    }
}
