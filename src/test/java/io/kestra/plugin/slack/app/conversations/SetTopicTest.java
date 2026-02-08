package io.kestra.plugin.slack.app.conversations;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.models.ConversationTopicOutput;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class SetTopicTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        SetTopic task = SetTopic.builder()
            .id(IdUtils.create())
            .type(SetTopic.class.getName())
            .slack(this.client("conversations"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("C1234567890"))
            .topic(Property.ofValue("New channel topic"))
            .build();

        ConversationTopicOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("channel=C1234567890");
        assertThat(FakeWebhookController.data).contains("topic=New%20channel%20topic");
    }
}
