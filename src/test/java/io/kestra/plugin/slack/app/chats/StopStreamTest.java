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
public class StopStreamTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        StopStream task = StopStream.builder()
            .id(IdUtils.create())
            .type(StopStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .timestamp(Property.ofValue("1234567890.123456"))
            .markdownText(Property.ofValue("Stream completed"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("ts=1234567890.123456");
        assertThat(FakeWebhookController.data).contains("markdown_text=Stream%20completed");
    }

    @Test
    void runWithBlocks() throws Exception {
        StopStream task = StopStream.builder()
            .id(IdUtils.create())
            .type(StopStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .timestamp(Property.ofValue("1234567890.123456"))
            .markdownText(Property.ofValue("Final message"))
            .blocks(Property.ofValue("""
                [
                  {
                    "type": "section",
                    "text": {
                      "type": "mrkdwn",
                      "text": "*Status:* Completed"
                    }
                  }
                ]
                """))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("ts=1234567890.123456");
        assertThat(FakeWebhookController.data).contains("markdown_text=Final%20message");
        assertThat(FakeWebhookController.data).contains("blocks=");
    }

    @Test
    void runWithMetadata() throws Exception {
        StopStream task = StopStream.builder()
            .id(IdUtils.create())
            .type(StopStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .timestamp(Property.ofValue("1234567890.123456"))
            .markdownText(Property.ofValue("Done"))
            .metadata(Property.ofValue("""
                {
                  "event_type": "task_completed",
                  "event_payload": {
                    "status": "success"
                  }
                }
                """))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("ts=1234567890.123456");
        assertThat(FakeWebhookController.data).contains("metadata=");
    }
}
