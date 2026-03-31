package io.kestra.plugin.slack.app.conversations;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.app.models.ConversationOutput;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class CreateTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Create task = Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversations"))
            .token(Property.ofValue("token"))
            .name(Property.ofValue("new-test-channel"))
            .isPrivate(Property.ofValue(false))
            .build();

        ConversationOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("name=new-test-channel");
        assertThat(FakeWebhookController.data).contains("is_private=0");
    }

    @Test
    void runPrivateChannel() throws Exception {
        Create task = Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversations"))
            .token(Property.ofValue("token"))
            .name(Property.ofValue("private-channel"))
            .isPrivate(Property.ofValue(true))
            .build();

        ConversationOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("name=private-channel");
        assertThat(FakeWebhookController.data).contains("is_private=1");
    }
}
