package io.kestra.plugin.slack.app.chats;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class StartStreamTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        StartStream task = StartStream.builder()
            .id(IdUtils.create())
            .type(StartStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .markdownText(Property.ofValue("Starting stream message"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("markdown_text=Starting%20stream%20message");
    }

    @Test
    void runWithThread() throws Exception {
        StartStream task = StartStream.builder()
            .id(IdUtils.create())
            .type(StartStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .markdownText(Property.ofValue("Stream in thread"))
            .timestamp(Property.ofValue("1234567890.123456"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("markdown_text=Stream%20in%20thread");
        assertThat(FakeWebhookController.data).contains("thread_ts=1234567890.123456");
    }

    @Test
    void runWithRecipient() throws Exception {
        StartStream task = StartStream.builder()
            .id(IdUtils.create())
            .type(StartStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("@user"))
            .markdownText(Property.ofValue("Direct message stream"))
            .recipientUserId(Property.ofValue("U1234567890"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40user");
        assertThat(FakeWebhookController.data).contains("markdown_text=Direct%20message%20stream");
        assertThat(FakeWebhookController.data).contains("recipient_user_id=U1234567890");
    }
}
