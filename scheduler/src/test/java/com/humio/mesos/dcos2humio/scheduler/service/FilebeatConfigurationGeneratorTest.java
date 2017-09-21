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

import java.io.File;
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
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    FilebeatConfigurationGenerator generator;

    @Mock
    MesosConfigProperties mesosConfigProperties;

    @Mock
    UniversalScheduler scheduler;

    @Mock
    Clock clock;

    @Before
    public void setUp() throws Exception {
        when(clock.instant()).thenReturn(Instant.ofEpochMilli(1505991592768L));
    }

    private State read() throws java.io.IOException {
        return objectMapper.readValue(new File("/Users/mwl/IdeaProjects/Humio/state.json"), State.class);
    }

    @Test
    public void canParseJson() throws Exception {
        final State state = read();
        assertThat(state.getId()).isEqualTo("3dc3fb85-9a67-4efc-86b0-b7086c909428");
    }

    @Test
    public void canFindAllRunningTasks() throws Exception {
        generator.pushState(read());

        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(scheduler).sendFrameworkMessage(eq("humioexecutor.2973b31d-a899-4fc7-8448-fbe160dc292b-S0"), eq("2973b31d-a899-4fc7-8448-fbe160dc292b-S0"), dataArgumentCaptor.capture());
        assertThat(parse(dataArgumentCaptor.getValue())).hasSize(12);

        verify(scheduler).sendFrameworkMessage(eq("humioexecutor.4da93dca-dbdb-4796-80f5-9dfd20e7a331-S1"), eq("4da93dca-dbdb-4796-80f5-9dfd20e7a331-S1"), dataArgumentCaptor.capture());
        assertThat(parse(dataArgumentCaptor.getValue())).hasSize(1);

        verify(scheduler).sendFrameworkMessage(eq("humioexecutor.4da93dca-dbdb-4796-80f5-9dfd20e7a331-S0"), eq("4da93dca-dbdb-4796-80f5-9dfd20e7a331-S0"), dataArgumentCaptor.capture());
        assertThat(parse(dataArgumentCaptor.getValue())).hasSize(8);
    }

    @Test
    public void willNotIncludeIgnoredTasks() throws Exception {
        final List<String> taskIds = getAllTaskIds();
        assertThat(taskIds).doesNotContain("7fb745ae-f999-4341-ae97-244a173c7327", "4ed81f39-0ecf-42bd-ba46-e33a664d0383");
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

    @Test
    public void willNotIncludeVeryOldTasks() throws Exception {

    }

    @Test
    public void willIncludeTasksOfRecentCompletedFrameworks() throws Exception {
        assertThat(getAllTaskIds()).contains("hello-0-server__d59d46ef-5b09-44df-bbee-00850886e66f", "world-0-server__9c327930-4864-4d76-b028-adea8fe436a1", "world-1-server__c196c8ae-c394-459c-81af-82f6e7727306");
    }

    @Test
    public void willIncludeRecentCompletedTasks() throws Exception {
        assertThat(getAllTaskIds()).contains("sleeper_201709211045359tarK.ff8d546f-9eb9-11e7-9a3d-e27ca593bc80");
    }

    private static List<TaskDetails> parse(byte[] data) {
        return SerializationUtils.deserialize(data);
    }
}