package com.humio.mesos.dcos2humio.executor;

import com.github.mustachejava.Mustache;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class HumioExecutor implements Executor {
    private Mustache mustache;
    private String slaveId;

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
        System.out.println("task.getData().toStringUtf8() = " + task.getData().toStringUtf8());
        final String humioHost = data[0];
        final String humioDataspace = data[1];
        final String humioIngestToken = data[2];
        updateFileBeatConfig(emptyList());
        new Thread(() -> {
            String[] command = {
                    "filebeat-5.5.2-linux-x86_64/filebeat",
                    "-e",
//                    "-E", "logging.level=debug",
                    "-c", "filebeat-5.5.2-linux-x86_64/filebeat.yml",
                    "-path.data=/var/humio/data",
                    "-E", "filebeat.config.prospectors.path=../config/humio.yaml",
                    "-E", "filebeat.config.prospectors.reload.enabled=true",
                    "-E", "filebeat.config.prospectors.reload.period=10s",
                    "-E", "output.elasticsearch.hosts=[\"https://" + humioHost + ":443/api/v1/dataspaces/" + humioDataspace + "/ingest/elasticsearch\"]",
                    "-E", "output.elasticsearch.username=" + humioIngestToken,
                    "-E", "output.elasticsearch.compression_level=5",
                    "-E", "output.elasticsearch.bulk_max_size=200"
            };
            final Process process;
            try {
                System.out.println("Starting " + Stream.of(command).collect(Collectors.joining(" ")));
                process = Runtime.getRuntime().exec(command);
                inputStreamForEach(System.out::println, process.getInputStream());
                inputStreamForEach(System.err::println, process.getErrorStream());

                process.waitFor();
                System.out.println("Command terminated with exit=" + process.exitValue());
                if (process.exitValue() == 0) {
                    driver.sendStatusUpdate(Protos.TaskStatus.newBuilder()
                            .setExecutorId(task.getExecutor().getExecutorId())
                            .setTaskId(task.getTaskId())
                            .setState(Protos.TaskState.TASK_FINISHED)
                            .build());
                } else {
                    driver.sendStatusUpdate(Protos.TaskStatus.newBuilder()
                            .setExecutorId(task.getExecutor().getExecutorId())
                            .setTaskId(task.getTaskId())
                            .setState(Protos.TaskState.TASK_FAILED)
                            .build());
                    throw new RuntimeException("Filebeat exited with value=" + process.exitValue());
                }
            } catch (IOException | InterruptedException e) {
                driver.sendStatusUpdate(Protos.TaskStatus.newBuilder()
                        .setExecutorId(task.getExecutor().getExecutorId())
                        .setTaskId(task.getTaskId())
                        .setState(Protos.TaskState.TASK_FAILED)
                        .build());
                e.printStackTrace();
            }

        }).start();

        driver.sendStatusUpdate(Protos.TaskStatus.newBuilder()
                .setExecutorId(task.getExecutor().getExecutorId())
                .setTaskId(task.getTaskId())
                .setState(Protos.TaskState.TASK_RUNNING)
                .build());
    }

    private static void inputStreamForEach(Consumer<String> consumer, InputStream inputStream) {
        new Thread(() -> new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer)).start();
    }


    @Override
    public void killTask(ExecutorDriver driver, Protos.TaskID taskId) {
        System.out.println("HumioExecutor.killTask");
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

    @Override
    public void shutdown(ExecutorDriver driver) {
        System.out.println("HumioExecutor.shutdown");
    }

    @Override
    public void error(ExecutorDriver driver, String message) {
        System.out.println("HumioExecutor.error message = " + message);
    }
}
