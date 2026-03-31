package io.kestra.plugin.slack.app.chats;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class StreamTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("${slack.bot-token}")
    private String botToken;

    @Test
    void run() throws Exception {
        // First, post a regular message to create a thread
        Post postMessage = Post.builder()
            .id(IdUtils.create())
            .type(Post.class.getName())
            .token(Property.ofValue(botToken))
            .channel(Property.ofValue("C0ACC6BT2GK"))
            .messageText(Property.ofValue("Starting streaming test"))
            .build();
        Post.Output postOutput = postMessage.run(TestsUtils.mockRunContext(runContextFactory, postMessage, Map.of()));

        // Now start the stream as a reply to that message
        StartStream start = StartStream.builder()
            .id(IdUtils.create())
            .type(StartStream.class.getName())
            .token(Property.ofValue(botToken))
            .channel(Property.ofValue("C0ACC6BT2GK"))
            .timestamp(Property.ofValue(postOutput.getTimestamp()))
            .recipientUserId(Property.ofValue("U01JA8ZTC07"))
            .recipientTeamId(Property.ofValue("T01JX6XH5KN"))
            .markdownText(Property.ofValue("Starting stream message"))
            .build();
        StartStream.Output startOutput = start.run(TestsUtils.mockRunContext(runContextFactory, start, Map.of()));

        AppendStream append = AppendStream.builder()
            .id(IdUtils.create())
            .type(AppendStream.class.getName())
            .token(Property.ofValue(botToken))
            .channel(Property.ofValue("C0ACC6BT2GK"))
            .timestamp(Property.ofValue(startOutput.getTimestamp()))
            .markdownText(Property.ofValue("Appending more content"))
            .build();
        AppendStream.Output appendOutput = append.run(TestsUtils.mockRunContext(runContextFactory, append, Map.of()));

        StopStream stop = StopStream.builder()
            .id(IdUtils.create())
            .type(StopStream.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue(botToken))
            .channel(Property.ofValue("C0ACC6BT2GK"))
            .timestamp(Property.ofValue(startOutput.getTimestamp()))
            .markdownText(Property.ofValue("Stream completed"))
            .build();

        StopStream.Output stopOutput = stop.run(TestsUtils.mockRunContext(runContextFactory, stop, Map.of()));

        assertThat(stopOutput).isNotNull();
    }
}
