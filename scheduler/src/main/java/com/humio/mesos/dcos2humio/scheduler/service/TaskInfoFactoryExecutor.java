package com.humio.mesos.dcos2humio.scheduler.service;

import com.containersolutions.mesos.scheduler.ExecutionParameters;
import com.containersolutions.mesos.scheduler.TaskInfoFactory;
import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.google.protobuf.ByteString;
import com.humio.mesos.dcos2humio.scheduler.model.HumioConfig;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Service
public class TaskInfoFactoryExecutor implements TaskInfoFactory {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfoFactoryExecutor.class);
    private final MesosConfigProperties mesosConfig;
    @Value("${spring.application.name}")
    protected String applicationName;
    @Value("${humio.host}")
    protected String humioHost;
    @Value("${humio.dataspace}")
    protected String humioDataspace;
    @Value("${humio.ingesttoken}")
    protected String humioIngesttoken;
    @Value("${humio.dataDir}")
    protected String humioDataDir;
    @Value("${humio.filebeat.configUrl:}")
    protected String filebeatConfigUrl;
    @Value("${humio.metricbeat.configUrl:}")
    protected String metricbeatConfigUrl;
    @Value("${humio.dcosAuthToken:}")
    protected String dcosAuthToken;
    @Value("${humio.metrics.container}")
    protected Boolean enableContainerMetrics;
    private final HumioConfig humioConfig;

    public TaskInfoFactoryExecutor(MesosConfigProperties mesosConfig, HumioConfig humioConfig) {
        this.mesosConfig = mesosConfig;
        this.humioConfig = humioConfig;
    }

    @Override
    public Protos.TaskInfo create(String taskId, Protos.Offer offer, List<Protos.Resource> resources, ExecutionParameters executionParameters) {
        logger.debug("Creating Mesos task for taskId={}", taskId);
        return Protos.TaskInfo.newBuilder()
                .setName(applicationName + ".task")
                .setSlaveId(offer.getSlaveId())
                .setTaskId(Protos.TaskID.newBuilder().setValue(taskId))
                .addAllResources(resources)
                .setData(ByteString.copyFromUtf8(
                        String.join(";",
                                humioHost,
                                humioDataspace,
                                humioIngesttoken,
                                humioDataDir,
                                defaultIfEmpty(filebeatConfigUrl, " "),
                                defaultIfEmpty(metricbeatConfigUrl, " ")
                        )))
                .setLabels(Protos.Labels.newBuilder().addLabels(createLabel("HUMIO_IGNORE", "true")).build())
                .setDiscovery(Protos.DiscoveryInfo.newBuilder()
                    .setName(applicationName)
                    .setVisibility(Protos.DiscoveryInfo.Visibility.FRAMEWORK)
                    .build())
                .setExecutor(Protos.ExecutorInfo.newBuilder()
                    .setName("humioexecutor")
                        .setData(ByteString.copyFromUtf8(
                                String.join(";",
                                        dcosAuthToken,
                                        enableContainerMetrics.toString(),
                                        humioConfig.getGlobalFields().entrySet().stream()
                                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                                                .collect(Collectors.joining(","))

                                )))
                    .setExecutorId(Protos.ExecutorID.newBuilder().setValue("humioexecutor." + offer.getSlaveId()
                        .getValue()).build())
                    .setCommand(Protos.CommandInfo.newBuilder()
                        .setValue("jdk*/jre/bin/java -Xms128m -Xmx256m -jar executor-*.jar")
                        .addAllUris(mesosConfig.getUri().stream().map(uri -> Protos.CommandInfo.URI
                            .newBuilder().setValue(uri).build()).collect(Collectors.toList()))
                        .build())
                    .build())
                .build();
    }

    private Protos.Label createLabel(String key, String value) {
        return Protos.Label.newBuilder().setKey(key).setValue(value).build();
    }
}
