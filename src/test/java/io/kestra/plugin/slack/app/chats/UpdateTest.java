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

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class UpdateTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Update task = Update.builder()
            .id(IdUtils.create())
            .type(Update.class.getName())
            .slack(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("@channel"))
            .timestamp(Property.ofValue(Instant.ofEpochSecond(1234567890L, 123456000L)))
            .messageText(Property.ofValue("Updated message *with some bold text*"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40channel");
        assertThat(FakeWebhookController.data).contains("ts=1234567890.123456");
        assertThat(FakeWebhookController.data).contains("text=Updated%20message%20*with%20some%20bold%20text*");
    }
}
