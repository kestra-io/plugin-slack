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
import io.kestra.plugin.slack.app.models.ConversationTopicOutput;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class SetPurposeTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        SetPurpose task = SetPurpose.builder()
            .id(IdUtils.create())
            .type(SetPurpose.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversations"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("C1234567890"))
            .purpose(Property.ofValue("New channel purpose"))
            .build();

        ConversationTopicOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("channel=C1234567890");
        assertThat(FakeWebhookController.data).contains("purpose=New%20channel%20purpose");
    }
}
