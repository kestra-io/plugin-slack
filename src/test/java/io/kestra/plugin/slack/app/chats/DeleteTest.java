package io.kestra.plugin.slack.app.chats;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.services.MessageService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class DeleteTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Delete task = Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .slack(this.client())
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("@channel"))
            .timestamp(Property.ofValue(MessageService.fromSlackTimestamp("2023-01-01T00:00:00Z")))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).isEqualTo("channel=%40channel&ts=1672531200");
    }
}
