package io.kestra.plugin.slack.app.chats;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.EnabledIfSlackTokenSet;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class PostEphemeralTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("${slack.bot-token:}")
    private String botToken;

    @Test
    void ephemeral() throws Exception {
        PostEphemeral task = PostEphemeral.builder()
            .id(IdUtils.create())
            .methodsEndpointUrlPrefix(this.client())
            .type(PostEphemeral.class.getName())
            .token(Property.ofValue("token"))
            .messageText(Property.ofValue("A message *with some bold text*"))
            .user(Property.ofValue("user"))
            .channel(Property.ofValue("@channel"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40channel&text=A%20message%20*with%20some%20bold%20text*&user=user&link_names=0");
    }

    @Test
    @EnabledIfSlackTokenSet
    void ephemeralReal() throws Exception {
        PostEphemeral task = PostEphemeral.builder()
            .id(IdUtils.create())
            .type(PostEphemeral.class.getName())
            .token(Property.ofValue(botToken))
            .messageText(Property.ofValue("A message *with some bold text*"))
            .user(Property.ofValue("U01JA8ZTC07"))
            .channel(Property.ofValue("C0ACC6BT2GK"))
            .build();

        PostEphemeral.Output run = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(run).isNotNull();
        assertThat(run.getTimestamp()).isNotNull();
    }
}
