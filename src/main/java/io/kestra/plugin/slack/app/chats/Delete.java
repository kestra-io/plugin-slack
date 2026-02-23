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
    title = "Delete a Slack channel message",
    description = "Permanently removes a message by channel and timestamp. Requires `chat:write`; deletion cannot be undone."
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
        title = "Channel containing the message",
        description = "Channel ID or name where the target message exists."
    )
    @NotNull
    protected Property<String> channel;

    @Schema(
        title = "Message timestamp to delete",
        description = "Slack `ts` of the message; must belong to the specified channel."
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
        @Schema(title = "Timestamp of deleted message")
        @NotNull
        String timestamp;
    }
}
