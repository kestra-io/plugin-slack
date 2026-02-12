package io.kestra.plugin.slack.app.chats;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class AppendStreamTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        AppendStream task = AppendStream.builder()
            .id(IdUtils.create())
            .type(AppendStream.class.getName())
            .slack(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .timestamp(Property.ofValue("1234567890.123456"))
            .markdownText(Property.ofValue("Appending more content"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("ts=1234567890.123456");
        assertThat(FakeWebhookController.data).contains("markdown_text=Appending%20more%20content");
    }

    @Test
    void runWithNewline() throws Exception {
        AppendStream task = AppendStream.builder()
            .id(IdUtils.create())
            .type(AppendStream.class.getName())
            .slack(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .timestamp(Property.ofValue("1234567890.123456"))
            .markdownText(Property.ofValue("\nNew line content"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%23general");
        assertThat(FakeWebhookController.data).contains("ts=1234567890.123456");
        assertThat(FakeWebhookController.data).contains("markdown_text=%0ANew%20line%20content");
    }
}
