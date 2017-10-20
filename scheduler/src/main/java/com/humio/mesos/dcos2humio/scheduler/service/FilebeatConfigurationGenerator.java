package com.humio.mesos.dcos2humio.scheduler.service;

import com.containersolutions.mesos.scheduler.UniversalScheduler;
import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.humio.mesos.dcos2humio.scheduler.model.ModelUtils;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.*;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FilebeatConfigurationGenerator implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(FilebeatConfigurationGenerator.class);
    private final MesosConfigProperties mesosConfigProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final UniversalScheduler universalScheduler;
    private final Clock clock;
    private RestTemplate restTemplate;

    public FilebeatConfigurationGenerator(MesosConfigProperties mesosConfigProperties, RestTemplateBuilder restTemplateBuilder, UniversalScheduler universalScheduler, Clock clock) {
        this.mesosConfigProperties = mesosConfigProperties;
        this.universalScheduler = universalScheduler;
        this.clock = clock;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    private static Stream<Framework> getAllFrameworkStreams(State state) {
        return Stream.of(state.getFrameworks(), state.getCompletedFrameworks(), state.getUnregistreredFrameworks()).flatMap(Collection::stream);
    }

    private static Stream<Task> getAllStateStreams(Framework framework) {
        return Stream.of(framework.getTasks(), framework.getCompleted_tasks()).flatMap(Collection::stream);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.restTemplate = restTemplateBuilder.rootUri("http://" + mesosConfigProperties.getMaster()).build();
    }

    @Scheduled(fixedDelay = 10000L)
    public void updateConfig() {
        logger.info("Updating Filebeat configuration");
        final ResponseEntity<State> stateEntity = restTemplate.getForEntity("/state", State.class);
        if (stateEntity.getStatusCode().isError()) {
            throw new RuntimeException("Failed to fetch Mesos state: " + stateEntity.getStatusCode().getReasonPhrase());
        }
        final State state = stateEntity.getBody();
        if (state == null) {
            throw new RuntimeException("Mesos state responded with empty body");
        }
        pushState(state);
    }

    void pushState(State state) {
        final Map<String, String> frameworkNameMap = getAllFrameworkStreams(state).collect(Collectors.toMap(Framework::getId, Framework::getName));

        extractTaskDetailsPerSlave(frameworkNameMap, state)
                .forEach((slaveId, taskDetails) -> {
                    logger.info("Updating config for {} task(s) on slave mesos-id={}", taskDetails.size(), slaveId);
                    pushConfig(slaveId, taskDetails);
                });
    }

    private Map<String, List<TaskDetails>> extractTaskDetailsPerSlave(Map<String, String> frameworkNameMap, State state) {
        return getAllFrameworkStreams(state)
                .flatMap(FilebeatConfigurationGenerator::getAllStateStreams)
                .filter(task -> task.getLabels().stream().filter(label -> "HUMIO_IGNORE".equals(label.getKey())).map(Label::getValue).noneMatch(Boolean::parseBoolean))
                .filter(this::wasTaskRecentlyRunning)
                .map((Task task) -> {
                    final TaskDetails.TaskDetailsBuilder taskDetailsBuilder = ModelUtils.from(task)
                            .frameworkName(frameworkNameMap.get(task.getFrameworkId()))
                            .type(task.getLabels().stream().filter(label -> label.getKey().equalsIgnoreCase("HUMIO_TYPE")).map(Label::getValue).findFirst().orElse("kv"))
                            .serviceId(Optional.ofNullable(task.getDiscovery()).map(discovery -> "/" + discovery.getName()).orElse(null));
                    if (task.getLabels().stream().anyMatch(label -> label.getKey().equals("DCOS_PACKAGE_NAME") && label.getValue().equals("marathon-lb"))) {
                        taskDetailsBuilder
                                .type("marathon-lb")
                                .multilineNegate(true)
                                .multilineMatch("after")
                                .multilinePattern("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3}\\s")
                        ;
                    }
                    return taskDetailsBuilder.build();
                })
                .collect(Collectors.groupingBy(TaskDetails::getSlaveId));
    }

    private boolean wasTaskRecentlyRunning(Task task) {
        if (task.getState().equals("TASK_RUNNING")) {
            return true;
        }
        final Instant deadline = clock.instant().minus(Duration.ofHours(1));
        return Instant.ofEpochSecond(task.getStatuses().get(task.getStatuses().size() - 1).getTimestamp().longValue()).isAfter(deadline);

    }

    private void pushConfig(String slaveId, List<TaskDetails> taskDetails) {
        final byte[] data = SerializationUtils.serialize(((Serializable) taskDetails));

        universalScheduler.sendFrameworkMessage("humioexecutor." + slaveId, slaveId, data);
    }
}
