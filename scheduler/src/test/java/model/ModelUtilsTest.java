package model;

import org.assertj.core.util.Lists;
import org.junit.Test;

import com.humio.mesos.dcos2humio.scheduler.model.ModelUtils;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.ContainerId;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.ContainerStatus;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.Status;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.Task;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelUtilsTest {

    @Test
    public void taskWithContainerId() {
        ContainerStatus containerStatus = new ContainerStatus();
        ContainerId containerId = new ContainerId();
        containerId.setValue("edae7b18-a73d-4ee8-833f-336fc8ad5633");
        containerStatus.setContainerId(containerId);
        Task task = setTask();
        Status status = new Status();
        status.setState("TASK_RUNNING");
        status.setContainerStatus(containerStatus);
        task.setStatuses(Lists.newArrayList(status));
        TaskDetails taskDetails = ModelUtils.from(task).build();

        assertThat(taskDetails.getContainerId()).isEqualTo(containerId.getValue());
    }

    @Test
    public void taskWithNullContainerId() {
        Task task = setTask();
        Status status = new Status();
        status.setState("TASK_KILLED");
        task.setStatuses(Lists.newArrayList(status));
        TaskDetails taskDetails = ModelUtils.from(task).build();

        assertThat(taskDetails.getContainerId()).isNull();
    }

    private Task setTask() {
        Task task = new Task();
        task.setId("active-framework-1.recent-finished-task");
        task.setSlaveId("private-agent-1");
        task.setFrameworkId("active-framework-1");
        return task;
    }

}
