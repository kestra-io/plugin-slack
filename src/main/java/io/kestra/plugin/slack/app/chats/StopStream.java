package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatStopStreamRequest;
import com.slack.api.methods.response.chat.ChatStopStreamResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Stop a streaming conversation in a Slack channel.",
    description = "Ends a streaming message started with StartStream and finalizes its content. " +
        "Once stopped, the stream cannot be appended to anymore. " +
        "You need the `chat:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Stop a stream with final message",
            full = true,
            code = """
                id: slack_stop_stream
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
                    markdownText: "\\\\nStep completed"

                  - id: stop_stream
                    type: io.kestra.plugin.slack.app.chats.StopStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\n✅ All done!"
                """
        ),
        @Example(
            title = "Complete stream workflow",
            full = true,
            code = """
                id: slack_complete_stream
                namespace: company.team

                tasks:
                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    markdownText: "Starting data processing"

                  - id: process_data
                    type: io.kestra.plugin.core.flow.Sleep
                    duration: PT5S

                  - id: append_result
                    type: io.kestra.plugin.slack.app.chats.AppendStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\nProcessed 1000 records"

                  - id: stop_stream
                    type: io.kestra.plugin.slack.app.chats.StopStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\nProcessing completed at {{ now() }}"
                """
        ),
        @Example(
            title = "Stop stream with blocks",
            full = true,
            code = """
                id: slack_stop_stream_blocks
                namespace: company.team

                tasks:
                  - id: start_stream
                    type: io.kestra.plugin.slack.app.chats.StartStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    markdownText: "Running workflow..."

                  - id: stop_stream
                    type: io.kestra.plugin.slack.app.chats.StopStream
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "{{ outputs.start_stream.channel }}"
                    timestamp: "{{ outputs.start_stream.timestamp }}"
                    markdownText: "\\\\nWorkflow completed"
                    blocks: |
                      [
                        {
                          "type": "section",
                          "text": {
                            "type": "mrkdwn",
                            "text": "*Status:* :white_check_mark: Completed"
                          }
                        }
                      ]
                """
        )
    }
)
public class StopStream extends AbstractSlackClientConnection implements RunnableTask<StopStream.Output> {
    @Schema(
        title = "The channel ID where the stream is active.",
        description = "Must match the channel where the stream was started."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "The timestamp of the stream message to stop.",
        description = "This is the timestamp returned by the StartStream task."
    )
    @NotNull
    private Property<String> timestamp;

    @Schema(
        title = "The final markdown text to append before stopping the stream.",
        description = "Optional final content to add to the stream message."
    )
    private Property<String> markdownText;

    @Schema(
        title = "Block Kit blocks to include in the final message.",
        description = "Provide as a JSON array string."
    )
    private Property<String> blocks;

    @Schema(
        title = "Message metadata to include in the final message.",
        description = "Provide as a JSON object string."
    )
    private Property<String> metadata;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatStopStreamRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .ts(runContext.render(this.timestamp).as(String.class).orElseThrow());

        runContext.render(this.markdownText).as(String.class).ifPresent(builder::markdownText);
        runContext.render(this.blocks).as(String.class).ifPresent(builder::blocksAsString);
        runContext.render(this.metadata).as(String.class).ifPresent(builder::metadataAsString);

        ChatStopStreamResponse response = call(runContext, (client) -> client.chatStopStream(builder.build()));

        return StopStream.Output.builder()
            .timestamp(response.getTs())
            .channel(response.getChannel())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the stream message.", description = "May be null if not returned by the Slack API.")
        String timestamp;

        @Schema(title = "The channel where the stream was stopped.", description = "May be null if not returned by the Slack API.")
        String channel;
    }
}
