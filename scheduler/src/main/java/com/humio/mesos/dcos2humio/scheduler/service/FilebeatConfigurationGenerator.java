package com.humio.mesos.dcos2humio.scheduler.service;

import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.Task;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FilebeatConfigurationGenerator {
    private static final Logger logger = LoggerFactory.getLogger(FilebeatConfigurationGenerator.class);

    private final MesosConfigProperties mesosConfigProperties;
    private final RestTemplate restTemplate;

    public FilebeatConfigurationGenerator(MesosConfigProperties mesosConfigProperties, RestTemplateBuilder restTemplateBuilder) {
        this.mesosConfigProperties = mesosConfigProperties;
        this.restTemplate = restTemplateBuilder.rootUri("http://" + mesosConfigProperties.getMaster()).build();
    }

    @Scheduled(fixedDelay = 10000L)
    public void updateConfig() {
        logger.info("Updating Filebeat configuration");
        final ResponseEntity<Tasks> tasksEntity = restTemplate.getForEntity("/tasks", Tasks.class);
        tasksEntity.getBody().getTasks().stream().filter(task -> task.getState().equals("TASK_RUNNING")).map(Task::getFrameworkId).distinct().forEach(frameworkId -> logger.info("frameworkId={}", frameworkId));
    }
}
