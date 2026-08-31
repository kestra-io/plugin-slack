package io.kestra.plugin.slack.notifications;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.TestRunner;
import io.kestra.plugin.slack.AbstractSlackTest;
import io.kestra.plugin.slack.FakeWebhookController;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KestraTest
class SlackExecutionTest extends AbstractSlackTest {
    @Inject
    protected TestRunner runner;

    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;

    @BeforeAll
    protected void init() throws IOException, URISyntaxException {
        repositoryLoader.load(Objects.requireNonNull(SlackExecutionTest.class.getClassLoader().getResource("flows")));
        this.runner.run();
    }

    @Test
    void flow() throws Exception {
        var execution = runAndCaptureExecution(
            "main-flow-that-fails",
            "slack",
            Map.of("url", embeddedServer.getURL().toString())
        );

        String receivedData = waitForWebhookData(
            () -> FakeWebhookController.data != null && FakeWebhookController.data.contains(execution.getId()) ? FakeWebhookController.data : null,
            5000
        );

        assertThat(receivedData, containsString(execution.getId()));
        assertThat(receivedData, containsString("https://mysuperhost.com/kestra/ui"));
        assertThat(receivedData, containsString("Failed on task `failed`"));
        assertThat(receivedData, containsString("{\"title\":\"Env\",\"value\":\"DEV\",\"short\":true}"));
        assertThat(receivedData, containsString("{\"title\":\"Cloud\",\"value\":\"GCP\",\"short\":true}"));
        assertThat(receivedData, containsString("{\"title\":\"Final task ID\",\"value\":\"failed\",\"short\":true}"));
        assertThat(receivedData, containsString("myCustomMessage"));
    }

    @Test
    void flow_successfullFlowShowLastTaskId() throws Exception {
        var execution = runAndCaptureExecution(
            "main-flow-that-succeeds",
            "slack-successful",
            Map.of("url", embeddedServer.getURL().toString())
        );

        String receivedData = waitForWebhookData(
            () -> FakeWebhookController.data != null && FakeWebhookController.data.contains(execution.getId()) ? FakeWebhookController.data : null,
            5000
        );

        assertThat(receivedData, containsString(execution.getId()));
        assertThat(receivedData, containsString("https://mysuperhost.com/kestra/ui"));
        assertThat(receivedData, not(containsString("Failed on task `success`")));
        assertThat(receivedData, containsString("{\"title\":\"Final task ID\",\"value\":\"success\",\"short\":true}"));
    }
}
