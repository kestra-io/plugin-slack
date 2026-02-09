package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatDeleteRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a message from a channel.",
    description = "Delete a message from a Slack channel. This action is permanent and cannot be undone. " +
        "You need the `chat:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a message",
            full = true,
            code = """
                id: slack_delete_message
                namespace: company.team

                tasks:
                  - id: delete_message
                    type: io.kestra.plugin.slack.app.chats.Delete
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    timestamp: "{{ outputs.previous_task.timestamp }}"
                """
        ),
        @Example(
            title = "Post and delete a message",
            full = true,
            code = """
                id: slack_post_and_delete
                namespace: company.team

                tasks:
                  - id: post_message
                    type: io.kestra.plugin.slack.app.chats.Post
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    messageText: "Temporary message"

                  - id: delete_message
                    type: io.kestra.plugin.slack.app.chats.Delete
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    timestamp: "{{ outputs.post_message.timestamp }}"
                """
        )
    }
)
public class Delete extends AbstractSlackClientConnection implements RunnableTask<Delete.Output> {
    @Schema(
        title = "The channel ID where the message should be removed.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    protected Property<String> channel;

    @Schema(
        title = "The timestamp of the message to remove.",
        description = "The timestamp is returned when posting a message and uniquely identifies the message within the channel."
    )
    @NotNull
    protected Property<Instant> timestamp;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatDeleteRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .ts(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElseThrow());

        var response = call(runContext, (client) -> client.chatDelete(builder.build()));

        return Delete.Output.builder()
            .timestamp(response.getTs())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the posted message.")
        @NotNull
        String timestamp;
    }
}
