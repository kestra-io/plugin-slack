package io.kestra.plugin.slack.notifications;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.junit.annotations.LoadFlows;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.flows.State;
import io.kestra.core.queues.QueueException;
import io.kestra.core.runners.FlowInputOutput;
import io.kestra.core.runners.TestRunnerUtils;
import io.kestra.plugin.slack.EnabledIfSlackTokenSet;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static io.kestra.core.tenant.TenantService.MAIN_TENANT;
import static org.assertj.core.api.Assertions.assertThat;

@KestraTest(startRunner = true)
@EnabledIfSlackTokenSet
class RunnerTest {
    @Inject
    TestRunnerUtils runnerUtils;

    @Value("${slack.bot-token:}")
    private String botToken;

    @Inject
    private FlowInputOutput flowIO;

    @Test
    @LoadFlows(value = { "sanity-checks/chat-reactions.yaml" })
    void chat() throws QueueException, TimeoutException {
        Execution execution = runnerUtils.runOne(
            MAIN_TENANT,
            "sanitychecks.plugin-slack",
            "chat-reactions",
            null,
            (flow, exec) -> flowIO.readExecutionInputs(flow, exec, Map.of("token", botToken))
        );

        assertThat(execution).isNotNull();
        assertThat(execution.getState().getCurrent()).isEqualTo(State.Type.SUCCESS);
        assertThat(execution.getTaskRunList()).hasSize(9);
    }

    @Test
    @LoadFlows(value = { "sanity-checks/file.yaml" })
    void file() throws QueueException, TimeoutException {
        Execution execution = runnerUtils.runOne(
            MAIN_TENANT,
            "sanitychecks.plugin-slack",
            "file",
            null,
            (flow, exec) -> flowIO.readExecutionInputs(flow, exec, Map.of("token", botToken))
        );

        assertThat(execution).isNotNull();
        assertThat(execution.getState().getCurrent()).isEqualTo(State.Type.SUCCESS);
        assertThat(execution.getTaskRunList()).hasSize(5);
    }

    @Test
    @LoadFlows(value = { "sanity-checks/chat-stream.yaml" })
    void chatStream() throws QueueException, TimeoutException {
        Execution execution = runnerUtils.runOne(
            MAIN_TENANT,
            "sanitychecks.plugin-slack",
            "chat-stream",
            null,
            (flow, exec) -> flowIO.readExecutionInputs(flow, exec, Map.of("token", botToken))
        );

        assertThat(execution).isNotNull();
        assertThat(execution.getState().getCurrent()).isEqualTo(State.Type.SUCCESS);
        assertThat(execution.getTaskRunList()).hasSize(5);
    }

    @Test
    @LoadFlows(value = { "sanity-checks/chat-schedule.yaml" })
    void chatSchedule() throws QueueException, TimeoutException {
        Execution execution = runnerUtils.runOne(
            MAIN_TENANT,
            "sanitychecks.plugin-slack",
            "chat-schedule",
            null,
            (flow, exec) -> flowIO.readExecutionInputs(flow, exec, Map.of("token", botToken))
        );

        assertThat(execution).isNotNull();
        assertThat(execution.getState().getCurrent()).isEqualTo(State.Type.SUCCESS);
        assertThat(execution.getTaskRunList()).hasSize(3);
    }

    @Test
    @LoadFlows(value = { "sanity-checks/user.yaml" })
    void user() throws QueueException, TimeoutException {
        Execution execution = runnerUtils.runOne(
            MAIN_TENANT,
            "sanitychecks.plugin-slack",
            "user",
            null,
            (flow, exec) -> flowIO.readExecutionInputs(flow, exec, Map.of("token", botToken))
        );

        assertThat(execution).isNotNull();
        assertThat(execution.getState().getCurrent()).isEqualTo(State.Type.SUCCESS);
        assertThat(execution.getTaskRunList()).hasSize(6);
    }
}
