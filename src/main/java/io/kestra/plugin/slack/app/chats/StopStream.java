package io.kestra.plugin.slack.app.chats;

import com.slack.api.methods.request.chat.ChatStopStreamRequest;
import com.slack.api.methods.response.chat.ChatStopStreamResponse;

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
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Stop a Slack stream",
    description = "Finalizes a streaming message started with StartStream; no further appends are allowed after this. Optional final markdown, blocks, or metadata can be included. Requires `chat:write`."
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
        title = "Stream channel",
        description = "Channel ID or name where the stream was started."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> channel;

    @Schema(
        title = "Stream message timestamp",
        description = "Slack `ts` returned by StartStream."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> timestamp;

    @Schema(
        title = "Final markdown content",
        description = "Optional markdown appended before closing the stream."
    )
    @PluginProperty(group = "advanced")
    private Property<String> markdownText;

    @Schema(
        title = "Final Block Kit blocks",
        description = "JSON array string of blocks appended before closing."
    )
    @PluginProperty(group = "advanced")
    private Property<String> blocks;

    @Schema(
        title = "Final message metadata",
        description = "JSON object string attached to the closing message."
    )
    @PluginProperty(group = "advanced")
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
        @Schema(title = "Stream message timestamp", description = "May be null if not returned by the Slack API.")
        String timestamp;

        @Schema(title = "Stream channel", description = "May be null if not returned by the Slack API.")
        String channel;
    }
}
