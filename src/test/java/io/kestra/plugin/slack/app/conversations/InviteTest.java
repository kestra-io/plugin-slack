package io.kestra.plugin.slack.app.conversations;

import java.util.List;
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
public class InviteTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Invite task = Invite.builder()
            .id(IdUtils.create())
            .type(Invite.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversations"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("C1234567890"))
            .users(Property.ofValue(List.of("U1234567890", "U0987654321")))
            .build();

        ConversationOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("channel=C1234567890");
        assertThat(FakeWebhookController.data).contains("users=U1234567890");
    }
}
