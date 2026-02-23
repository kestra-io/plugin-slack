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
    title = "Append to a Slack stream",
    description = "Adds markdown to an active streaming message started with StartStream. Requires the original channel and timestamp and `chat:write`; fails if the stream is already stopped."
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
        title = "Stream channel",
        description = "Channel ID or name where the stream was started."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "Stream message timestamp",
        description = "Slack `ts` returned by StartStream."
    )
    @NotNull
    private Property<String> timestamp;

    @Schema(
        title = "Markdown to append",
        description = "Additional markdown content appended to the stream."
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
        @Schema(title = "Stream message timestamp")
        @NotNull
        String timestamp;

        @Schema(title = "Stream channel")
        @NotNull
        String channel;
    }
}
