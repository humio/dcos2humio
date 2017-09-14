package com.humio.mesos.dcos2humio.scheduler.service;

import com.containersolutions.mesos.scheduler.ExecutionParameters;
import com.containersolutions.mesos.scheduler.TaskInfoFactory;
import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.google.protobuf.ByteString;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class TaskInfoFactoryExecutor implements TaskInfoFactory {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfoFactoryExecutor.class);
    private final MesosConfigProperties mesosConfig;
    private final Supplier<UUID> uuidSupplier;
    @Value("${spring.application.name}")
    protected String applicationName;
    @Value("${humio.host}")
    protected String humioHost;
    @Value("${humio.dataspace}")
    protected String humioDataspace;
    @Value("${humio.ingesttoken}")
    protected String humioIngesttoken;

    public TaskInfoFactoryExecutor(MesosConfigProperties mesosConfig, Supplier<UUID> uuidSupplier) {
        this.mesosConfig = mesosConfig;
        this.uuidSupplier = uuidSupplier;
    }

    @Override
    public Protos.TaskInfo create(String taskId, Protos.Offer offer, List<Protos.Resource> resources, ExecutionParameters executionParameters) {
        logger.debug("Creating Mesos task for taskId={}", taskId);
        return Protos.TaskInfo.newBuilder()
                .setName(applicationName + ".task")
                .setSlaveId(offer.getSlaveId())
                .setTaskId(Protos.TaskID.newBuilder().setValue(taskId))
                .addAllResources(resources)
                .setData(ByteString.copyFromUtf8(String.join(";", humioHost, humioDataspace, humioIngesttoken)))
                .setLabels(Protos.Labels.newBuilder().addLabels(createLabel("HUMIO_IGNORE", "true")).build())
                .setExecutor(Protos.ExecutorInfo.newBuilder()
                        .setName("humioexecutor")
                        .setLabels(Protos.Labels.newBuilder().addLabels(createLabel("DCOS_SPACE", "/" + applicationName)).build())
                        .setExecutorId(Protos.ExecutorID.newBuilder().setValue("humioexecutor." + offer.getSlaveId().getValue()).build())
                        .setCommand(Protos.CommandInfo.newBuilder()
                                .setValue("jre*/bin/java -jar executor-*.jar")
                                .addAllUris(mesosConfig.getUri().stream().map(uri -> Protos.CommandInfo.URI.newBuilder().setValue(uri).build()).collect(Collectors.toList()))
                                .build())
                        .build())
                .build();
    }

    private Protos.Label createLabel(String key, String value) {
        return Protos.Label.newBuilder().setKey(key).setValue(value).build();
    }
}
