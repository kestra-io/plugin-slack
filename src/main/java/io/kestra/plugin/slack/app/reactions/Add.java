package io.kestra.plugin.slack.app.reactions;


import com.slack.api.methods.request.reactions.ReactionsAddRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Add a reaction to a message.",
    description = "Add an emoji reaction to a Slack message. " +
        "You need the `reactions:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Add a reaction to a message",
            full = true,
            code = """
                id: slack_add_reaction
                namespace: company.team

                tasks:
                  - id: add_reaction
                    type: io.kestra.plugin.slack.app.reactions.Add
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    name: "thumbsup"
                    timestamp: "{{ outputs.previous_task.timestamp }}"
                """
        ),
        @Example(
            title = "Post message and add reaction",
            full = true,
            code = """
                id: slack_post_with_reaction
                namespace: company.team

                tasks:
                  - id: post_message
                    type: io.kestra.plugin.slack.app.chats.Post
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    messageText: "Task completed successfully"

                  - id: add_reaction
                    type: io.kestra.plugin.slack.app.reactions.Add
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    name: "white_check_mark"
                    timestamp: "{{ outputs.post_message.timestamp }}"
                """
        )
    }
)
public class Add extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Channel, private group, or IM channel containing the message.",
        description = "Can be an encoded ID or a name. To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    protected Property<String> channel;

    @Schema(
        title = "Reaction (emoji) name.",
        description = "The name of the emoji without colons (e.g., 'thumbsup', 'white_check_mark', 'heart')."
    )
    @NotNull
    protected Property<String> name;

    @Schema(
        title = "Timestamp of the message to add reaction to.",
        description = "The timestamp uniquely identifies the message within the channel."
    )
    @NotNull
    protected Property<Instant> timestamp;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ReactionsAddRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .name(runContext.render(this.name).as(String.class).orElseThrow())
            .timestamp(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElseThrow());

        call(runContext, (client) -> client.reactionsAdd(builder.build()));

        return null;
    }
}
