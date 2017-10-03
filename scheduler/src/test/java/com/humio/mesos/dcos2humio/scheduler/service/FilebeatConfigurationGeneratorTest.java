package com.humio.mesos.dcos2humio.scheduler.service;

import com.containersolutions.mesos.scheduler.UniversalScheduler;
import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humio.mesos.dcos2humio.scheduler.model.mesos.State;
import com.humio.mesos.dcos2humio.shared.model.TaskDetails;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilebeatConfigurationGeneratorTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private FilebeatConfigurationGenerator generator;

    @Mock
    private UniversalScheduler scheduler;

    @Mock
    private Clock clock;

    private static List<TaskDetails> parse(byte[] data) {
        return SerializationUtils.deserialize(data);
    }

    @Before
    public void setUp() throws Exception {
        when(clock.instant()).thenReturn(Instant.ofEpochMilli(1505991592768L));
    }

    private State read() throws java.io.IOException {
        return objectMapper.readValue(FilebeatConfigurationGeneratorTest.class.getResourceAsStream("/state.json"), State.class);
    }

    @Test
    public void tasksAreBeingSentToSlaves() throws Exception {
        generator.pushState(read());
        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(scheduler).sendFrameworkMessage(eq("humioexecutor.private-agent-1"), eq("private-agent-1"), dataArgumentCaptor.capture());
        assertThat(parse(dataArgumentCaptor.getValue()).stream().map(TaskDetails::getTaskId)).hasSize(3);

        verify(scheduler).sendFrameworkMessage(eq("humioexecutor.public-agent-1"), eq("public-agent-1"), dataArgumentCaptor.capture());
        assertThat(parse(dataArgumentCaptor.getValue()).stream().map(TaskDetails::getTaskId)).hasSize(1);
    }

    @Test
    public void willIncludeActiveTasks() throws Exception {
        assertThat(getAllTaskIds()).contains("active-framework-2.active-task");
    }

    @Test
    public void willNotIncludeIgnoredTasks() throws Exception {
        assertThat(getAllTaskIds()).doesNotContain("active-framework-2.active-ignored-task");
    }

    @Test
    public void willNotIncludeVeryOldCompletedTasks() throws Exception {
        assertThat(getAllTaskIds()).doesNotContain(
                "active-framework-1.ancient-finished-task",
                "active-framework-2.ancient-killed-scheduler-task",
                "completed-framework-1.ancient-killed-task"
        );
    }

    @Test
    public void willIncludeTasksOfRecentCompletedFrameworks() throws Exception {
        assertThat(getAllTaskIds()).contains("completed-framework-1.recent-killed-task");
    }

    @Test
    public void willIncludeRecentCompletedTasks() throws Exception {
        assertThat(getAllTaskIds()).contains(
                "active-framework-1.recent-finished-task",
                "active-framework-2.recent-killed-scheduler-task",
                "completed-framework-1.recent-killed-task"
        );
    }

    @Test
    public void willRecogniseMarathonLbTasksAndAddMultilineLabels() throws Exception {
        generator.pushState(read());
        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(scheduler, atLeastOnce()).sendFrameworkMessage(any(), any(), dataArgumentCaptor.capture());
        final TaskDetails marathonLbTask = dataArgumentCaptor.getAllValues().stream()
                .map(FilebeatConfigurationGeneratorTest::parse)
                .flatMap(Collection::stream)
                .filter(taskDetails -> taskDetails.getTaskId().equals("active-framework-2.active-task"))
                .findAny()
                .orElseThrow(AssertionError::new);
        assertThat(marathonLbTask.getType()).isEqualTo("marathon-lb");
        assertThat(marathonLbTask.isMultilineEnabled()).isTrue();
        assertThat(marathonLbTask.getMultilineMatch()).isEqualTo("after");
        assertThat(marathonLbTask.getMultilinePattern()).isEqualTo("^\\\\d{4}-\\\\d{2}-\\\\d{2}\\\\s\\\\d{2}:\\\\d{2}:\\\\d{2},\\\\d{3}\\\\s");
        assertThat(marathonLbTask.isMultilineNegate()).isTrue();
    }

    private List<String> getAllTaskIds() throws java.io.IOException {
        generator.pushState(read());
        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(scheduler, atLeastOnce()).sendFrameworkMessage(any(), any(), dataArgumentCaptor.capture());
        return dataArgumentCaptor.getAllValues().stream()
                .map(FilebeatConfigurationGeneratorTest::parse)
                .flatMap(Collection::stream)
                .map(TaskDetails::getTaskId)
                .collect(Collectors.toList());
    }
}