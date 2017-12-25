package com.humio.mesos.dcos2humio.executor;

import com.github.mustachejava.Mustache;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class HumioExecutor implements Executor {
    AtomicBoolean runningFlag = new AtomicBoolean(true);
    private Mustache mustache;
    private String slaveId;
    private List<ProcessLauncher> processes;

    public HumioExecutor(Mustache mustache) {
        this.mustache = mustache;
    }

    @Override
    public void registered(ExecutorDriver driver, Protos.ExecutorInfo executorInfo, Protos.FrameworkInfo frameworkInfo, Protos.SlaveInfo slaveInfo) {
        slaveId = slaveInfo.getId().getValue();
        System.out.println("HumioExecutor.registered");
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

        updateFileBeatConfig(emptyList());

        processes = asList(
                new ProcessLauncher(filebeatWorkingDir,
                        "filebeat-5.6.3-linux-x86_64/filebeat", //TODO: upgrade to 5.6.3. Parametize?
                        "-c", filebeatConfig != null ? filebeatConfig.getAbsolutePath() : "filebeat-5.6.3-linux-x86_64/filebeat.yml",
                        "-path.data=" + filebeatDataDir.getAbsolutePath(),
                        "-E", "filebeat.config.prospectors.path=../config/humio.yaml",
                        "-E", "filebeat.config.prospectors.reload.enabled=true",
                        "-E", "filebeat.config.prospectors.reload.period=10s",
                        "-E", "output.elasticsearch.hosts=[\"https://" + humioHost + ":443/api/v1/dataspaces/" + humioDataspace + "/ingest/elasticsearch\"]",
                        "-E", "output.elasticsearch.username=" + humioIngestToken,
                        "-E", "output.elasticsearch.compression_level=5",
                        "-E", "output.elasticsearch.bulk_max_size=200"
                ),
                new ProcessLauncher(metricbeatWorkingDir, "metricbeat-5.6.3-linux-x86_64/metricbeat",
                        "-path.data=" + metricbeatDataDir.getAbsolutePath(),
                        "-c", metricbeatConfig != null ? metricbeatConfig.getAbsolutePath() : "metricbeat-5.6.3-linux-x86_64/metricbeat.yml",
                        "-E", "name=" + slaveId,
                        "-E", "output.elasticsearch.hosts=[\"https://" + humioHost + ":443/api/v1/dataspaces/" + humioDataspace + "/ingest/elasticsearch\"]",
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

        updateFileBeatConfig(taskDetails);
    }

    private void updateFileBeatConfig(List<TaskDetails> taskDetails) {
        try {
            mustache.execute(new FileWriter("config/humio.yaml"), new FilebeatConfigScope(slaveId, taskDetails)).flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File copyConfigFile(String configUrl, String filename) {
        File configFile = null;
        if (!configUrl.isEmpty()) {
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
