package io.kestra.plugin.slack.app.chats;

import java.time.Instant;
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
public class ScheduleTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Schedule task = Schedule.builder()
            .id(IdUtils.create())
            .type(Schedule.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("@channel"))
            .postAt(Property.ofValue(Instant.ofEpochSecond(1609459200)))
            .messageText(Property.ofValue("Scheduled message *with some bold text*"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40channel");
        assertThat(FakeWebhookController.data).contains("post_at=1609459200");
        assertThat(FakeWebhookController.data).contains("text=Scheduled%20message%20*with%20some%20bold%20text*");
    }
}
