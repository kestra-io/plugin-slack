package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatAppendStreamRequest;
import com.slack.api.methods.response.chat.ChatAppendStreamResponse;
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
    title = "Append text to a streaming conversation in a Slack channel.",
    description = "Appends additional content to an existing stream started with StartStream. " +
        "The stream must be active (not yet stopped with StopStream). " +
        "You need the `chat:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Append to an existing stream",
            full = true,
            code = """
                id: slack_append_stream
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
                    markdownText: "Step 1 completed successfully"
                """
        ),
        @Example(
            title = "Multiple appends to a stream",
            full = true,
            code = """
                id: slack_multi_append_stream
                namespace: company.team

                tasks:
                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    markdownText: "Starting data processing..."

                  - id: append_step1
                    type: io.kestra.plugin.slack.app.chats.AppendStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\nFetching data from source..."

                  - id: append_step2
                    type: io.kestra.plugin.slack.app.chats.AppendStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\nProcessing 1000 records..."

                  - id: stop_stream
                    type: io.kestra.plugin.slack.app.chats.StopStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\n✅ Processing complete!"
                """
        ),
        @Example(
            title = "Stream progress updates",
            full = true,
            code = """
                id: slack_stream_progress
                namespace: company.team

                tasks:
                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    markdownText: "Starting long-running process..."

                  - id: update_progress
                    type: io.kestra.plugin.slack.app.chats.AppendStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\nProgress: {{ taskrun.value }}%"
                """
        )
    }
)
public class AppendStream extends AbstractSlackClientConnection implements RunnableTask<AppendStream.Output> {
    @Schema(
        title = "The channel ID where the stream is active.",
        description = "Must match the channel where the stream was started."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "The timestamp of the stream message to append to.",
        description = "This is the timestamp returned by the StartStream task."
    )
    @NotNull
    private Property<String> timestamp;

    @Schema(
        title = "The markdown text to append to the stream.",
        description = "Additional content to add to the stream message."
    )
    @NotNull
    private Property<String> markdownText;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatAppendStreamRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .ts(runContext.render(this.timestamp).as(String.class).orElseThrow())
            .markdownText(runContext.render(this.markdownText).as(String.class).orElseThrow());

        ChatAppendStreamResponse response = call(runContext, (client) -> client.chatAppendStream(builder.build()));

        return AppendStream.Output.builder()
            .timestamp(response.getTs())
            .channel(response.getChannel())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the stream message.")
        @NotNull
        String timestamp;

        @Schema(title = "The channel where the stream is active.")
        @NotNull
        String channel;
    }
}
