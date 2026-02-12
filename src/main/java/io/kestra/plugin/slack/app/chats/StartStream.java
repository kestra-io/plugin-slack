package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatStartStreamRequest;
import com.slack.api.methods.response.chat.ChatStartStreamResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Start a streaming conversation in a Slack channel.",
    description = "Initiates a streaming message that can be appended to incrementally. " +
        "Use this task to start a stream, then use AppendStream to add content, and finally StopStream to complete it. " +
        "You need the `chat:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Start a stream with markdown text",
            full = true,
            code = """
                id: slack_start_stream
                namespace: company.team

                tasks:
                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    markdownText: "Starting analysis..."
                """
        ),
        @Example(
            title = "Start a stream in a thread",
            full = true,
            code = """
                id: slack_stream_thread
                namespace: company.team

                tasks:
                  - id: post_message
                    type: io.kestra.plugin.slack.app.chats.Post
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    messageText: "Starting new process"

                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    threadTs: "{{ outputs.post_message.timestamp }}"
                    markdownText: "Process details streaming..."
                """
        ),
        @Example(
            title = "Start a stream and append content",
            full = true,
            code = """
                id: slack_stream_workflow
                namespace: company.team

                tasks:
                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    markdownText: "Processing workflow..."

                  - id: append_stream
                    type: io.kestra.plugin.slack.app.chats.AppendStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "Step 1 completed"

                  - id: stop_stream
                    type: io.kestra.plugin.slack.app.chats.StopStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "All steps completed!"
                """
        )
    }
)
public class StartStream extends AbstractSlackClientConnection implements RunnableTask<StartStream.Output> {
    @Schema(
        title = "Channel, group, or IM channel to send stream to.",
        description = "Can be an encoded ID, or a name."
    )
    private Property<String> channel;

    @Schema(
        title = "The markdown text to include in the stream.",
        description = "Initial content for the stream message."
    )
    private Property<String> markdownText;

    @Schema(
        title = "Provide another message's timestamp value to make this stream a reply in a thread."
    )
    private Property<String> timestamp;

    @Schema(
        title = "The encoded ID of the user to receive the streaming text.",
        description = "Required when streaming to channels."
    )
    private Property<String> recipientUserId;

    @Schema(
        title = "The encoded ID of the team the user receiving the streaming text belongs to.",
        description = "Required when streaming to channels."
    )
    private Property<String> recipientTeamId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatStartStreamRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow());

        runContext.render(this.markdownText).as(String.class).ifPresent(builder::markdownText);
        runContext.render(this.timestamp).as(String.class).ifPresent(builder::threadTs);
        runContext.render(this.recipientUserId).as(String.class).ifPresent(builder::recipientUserId);
        runContext.render(this.recipientTeamId).as(String.class).ifPresent(builder::recipientTeamId);

        ChatStartStreamResponse response = call(runContext, (client) -> client.chatStartStream(builder.build()));

        return StartStream.Output.builder()
            .timestamp(response.getTs())
            .channel(response.getChannel())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the stream message.", description = "Used to reference this stream in AppendStream and StopStream tasks.")
        @NotNull
        String timestamp;

        @Schema(title = "The channel where the stream was started.")
        @NotNull
        String channel;
    }
}
