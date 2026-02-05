package io.kestra.plugin.slack.chats;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.reactions.Add;
import io.kestra.plugin.slack.reactions.Remove;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class PostTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Post task = Post.builder()
            .id(IdUtils.create())
            .slack(this.client())
            .type(Post.class.getName())
            .token(Property.ofValue("token"))
            .messageText(Property.ofValue("A message *with some bold text*"))
            .channel(Property.ofValue("@channel"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40channel&text=A%20message%20*with%20some%20bold%20text*&link_names=0&mrkdwn=1&unfurl_links=0&unfurl_media=0&reply_broadcast=0");
    }

    @Test
    void ephemeral() throws Exception {
        PostEphemeral task = PostEphemeral.builder()
            .id(IdUtils.create())
            .slack(this.client())
            .type(PostEphemeral.class.getName())
            .token(Property.ofValue("token"))
            .messageText(Property.ofValue("A message *with some bold text*"))
            .channel(Property.ofValue("@channel"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("channel=%40channel&text=A%20message%20*with%20some%20bold%20text*&link_names=0");
    }
}
