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
public class DeleteScheduledTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        DeleteScheduled task = DeleteScheduled.builder()
            .id(IdUtils.create())
            .type(DeleteScheduled.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("@channel"))
            .scheduledMessageId(Property.ofValue("Q1234ABCD"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40channel");
        assertThat(FakeWebhookController.data).contains("scheduled_message_id=Q1234ABCD");
    }
}
