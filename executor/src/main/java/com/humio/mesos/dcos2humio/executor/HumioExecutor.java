package com.humio.mesos.dcos2humio.executor;

import com.github.mustachejava.Mustache;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class HumioExecutor implements Executor {
    private static final String HUMIO_FILEBEAT_YAML = "config/filebeat.humio.yaml";
    private static final String HUMIO_METRICBEAT_YAML = "config/metricbeat.humio.yaml";

    AtomicBoolean runningFlag = new AtomicBoolean(true);
    private Mustache filebeatMustache;
    private Mustache metricbeatMustache;
    private String slaveId;
    private List<ProcessLauncher> processes;
    private String dcosAuthToken = null;

    private boolean metricsContainersEnabled = false;
    private List<LogField> globalFields = emptyList();

    public HumioExecutor(Mustache filebeatMustache, Mustache metricbeatMustache) {
        this.filebeatMustache = filebeatMustache;
        this.metricbeatMustache = metricbeatMustache;
    }

    @Override
    public void registered(ExecutorDriver driver, Protos.ExecutorInfo executorInfo, Protos.FrameworkInfo frameworkInfo, Protos.SlaveInfo slaveInfo) {
        final String[] bootConfig = executorInfo.getData().toStringUtf8().split(";");
        this.dcosAuthToken = nullOnEmpty(bootConfig[0]);
        this.metricsContainersEnabled = Boolean.parseBoolean(bootConfig[1]);
        globalFields = bootConfig.length < 3 ? emptyList() : Stream.of(bootConfig[2].split(","))
                .map(s -> s.split("="))
                .map(strings -> new LogField(strings[0], strings[1]))
                .collect(Collectors.toList());
        slaveId = slaveInfo.getId().getValue();
        System.out.println("HumioExecutor.registered");
    }

    private static String nullOnEmpty(String string) {
        return string == null || string.trim().length() == 0 ? null : string;
    }

    @Override
    public void reregistered(ExecutorDriver driver, Protos.SlaveInfo slaveInfo) {
        System.out.println("HumioExecutor.reregistered");
    }

    @Override
    public void disconnected(ExecutorDriver driver) {
        System.out.println("HumioExecutor.disconnected");
    }

    @Override
    public void launchTask(ExecutorDriver driver, Protos.TaskInfo task) {
        final String[] data = task.getData().toStringUtf8().split(";");
        final String humioHost = data[0];
        final String humioDataspace = data[1];
        final String humioIngestToken = data[2];
        final File dataDir = new File(data[3]);
        final File filebeatConfig = copyConfigFile(data[4], "filebeat.yaml");
        final File metricbeatConfig = copyConfigFile(data[5], "metricbeat.yaml");
        final File filebeatDataDir = new File(dataDir, "filebeat");
        final File metricbeatDataDir = new File(dataDir, "metricbeat");
        final File filebeatWorkingDir = new File(".", "filebeat");
        final File metricbeatWorkingDir = new File(".", "metricbeat");

        updateElasticBeatConfig(HUMIO_FILEBEAT_YAML, emptyList());
        updateElasticBeatConfig(HUMIO_METRICBEAT_YAML, emptyList());

        processes = asList(
                new ProcessLauncher(filebeatWorkingDir,
                        "filebeat-6.1.1-linux-x86_64/filebeat", //TODO: upgrade to 6.1.1. Parametize?
                        "-c", filebeatConfig != null ? filebeatConfig.getAbsolutePath() : "filebeat-6.1.1-linux-x86_64/filebeat.yml",
                        "-path.data=" + filebeatDataDir.getAbsolutePath(),
                        "-E", "filebeat.config.prospectors.path=../".concat(HUMIO_FILEBEAT_YAML),
                        "-E", "filebeat.config.prospectors.reload.enabled=true",
                        "-E", "filebeat.config.prospectors.reload.period=10s",
                        "-E", "output.elasticsearch.hosts=[\"" + humioHost + "/api/v1/dataspaces/" + humioDataspace + "/ingest/elasticsearch\"]",
                        "-E", "output.elasticsearch.username=" + humioIngestToken,
                        "-E", "output.elasticsearch.compression_level=5",
                        "-E", "output.elasticsearch.bulk_max_size=200"
                ),
                new ProcessLauncher(metricbeatWorkingDir, "metricbeat-6.1.1-linux-x86_64/metricbeat",
                        "-path.data=" + metricbeatDataDir.getAbsolutePath(),
                        "-c", metricbeatConfig != null ? metricbeatConfig.getAbsolutePath() : "metricbeat-6.1.1-linux-x86_64/metricbeat.yml",
                        "-E", "name=" + slaveId,
                        "-E", "metricbeat.config.modules.path=../".concat(HUMIO_METRICBEAT_YAML),
                        "-E", "metricbeat.config.modules.reload.enabled=true",
                        "-E", "metricbeat.config.modules.reload.period=10s",
                        "-E", "output.elasticsearch.hosts=[\"" + humioHost + "/api/v1/dataspaces/" + humioDataspace + "/ingest/elasticsearch\"]",
                        "-E", "output.elasticsearch.username=" + humioIngestToken
                )
        );

        sendStatusUpdate(driver, task, Protos.TaskState.TASK_RUNNING, false);

        new Thread(() -> {
            AtomicBoolean isHealthy = new AtomicBoolean(false);
            while (runningFlag.get()) {
                processes.stream()
                        .filter(ProcessLauncher::isNotRunning)
                        .peek(processLauncher -> {
                            if (processLauncher.hasFinished()) {
                                System.out.println("Process finished");
                            } else if (processLauncher.hasFailed()) {
                                System.out.println("Process failed with exitcode " + processLauncher.exitValue());
                            }
                        })
                        .forEach(ProcessLauncher::start);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                final boolean currentHealth = processes.stream().anyMatch(ProcessLauncher::isHealthy);
                if (currentHealth != isHealthy.get()) {
                    sendStatusUpdate(driver, task, Protos.TaskState.TASK_RUNNING, currentHealth);
                }
                isHealthy.set(currentHealth);
            }
            processes.forEach(ProcessLauncher::stop);
            sendStatusUpdate(driver, task, Protos.TaskState.TASK_FINISHED, false);
        }).start();
    }

    protected void sendStatusUpdate(ExecutorDriver driver, Protos.TaskInfo task, Protos.TaskState state, boolean healthy) {
        driver.sendStatusUpdate(Protos.TaskStatus.newBuilder()
            .setExecutorId(task.getExecutor().getExecutorId())
            .setTaskId(task.getTaskId())
            .setHealthy(healthy)
            .setState(state)
            .build());
    }

    @Override
    public void killTask(ExecutorDriver driver, Protos.TaskID taskId) {
        System.out.println("HumioExecutor.killTask");
        runningFlag.set(false);
        processes.forEach(ProcessLauncher::stop);
    }

    @Override
    public void frameworkMessage(ExecutorDriver driver, byte[] data) {
        System.out.println("HumioExecutor.frameworkMessage");
        final List<TaskDetails> taskDetails = SerializationUtils.deserialize(data);

        updateElasticBeatConfig(HUMIO_FILEBEAT_YAML, taskDetails);
        updateElasticBeatConfig(HUMIO_METRICBEAT_YAML, taskDetails.stream().filter(taskDetail -> taskDetail.getContainerId() != null).collect(Collectors.toList()));
    }

    private void updateElasticBeatConfig(String fileName, List<TaskDetails> taskDetails) {
        try {
            Mustache mustache = fileName.equals(HUMIO_FILEBEAT_YAML) ? filebeatMustache : metricbeatMustache;
            mustache.execute(new FileWriter(fileName), new ElasticBeatConfigScope(dcosAuthToken, slaveId, taskDetails, globalFields, metricsContainersEnabled)).flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File copyConfigFile(String configUrl, String filename) {
        File configFile = null;
        if (StringUtils.isNotBlank(configUrl)) {
            configFile = new File("config/".concat(filename));
            try {
                FileUtils.copyURLToFile(new URL(configUrl), configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return configFile;
    }

    @Override
    public void shutdown(ExecutorDriver driver) {
        System.out.println("HumioExecutor.shutdown");
        runningFlag.set(false);
        processes.forEach(ProcessLauncher::stop);
    }

    @Override
    public void error(ExecutorDriver driver, String message) {
        System.out.println("HumioExecutor.error message = " + message);
    }
}
