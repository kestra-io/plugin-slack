package io.kestra.plugin.slack.app.reactions;

import java.time.Instant;

import com.slack.api.methods.request.reactions.ReactionsRemoveRequest;

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
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Remove a Slack reaction",
    description = "Removes an emoji reaction from a message by channel and timestamp. Requires `reactions:write`."
)
@Plugin(
    examples = {
        @Example(
            title = "Remove a reaction from a message",
            full = true,
            code = """
                id: slack_remove_reaction
                namespace: company.team

                tasks:
                  - id: remove_reaction
                    type: io.kestra.plugin.slack.app.reactions.Remove
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    name: "thumbsup"
                    timestamp: "{{ outputs.previous_task.timestamp }}"
                """
        ),
        @Example(
            title = "Add and then remove a reaction",
            full = true,
            code = """
                id: slack_reaction_workflow
                namespace: company.team

                tasks:
                  - id: add_reaction
                    type: io.kestra.plugin.slack.app.reactions.Add
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    name: "hourglass"
                    timestamp: "{{ inputs.message_timestamp }}"

                  - id: remove_reaction
                    type: io.kestra.plugin.slack.app.reactions.Remove
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    name: "hourglass"
                    timestamp: "{{ inputs.message_timestamp }}"
                """
        )
    }
)
public class Remove extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Channel containing the message",
        description = "Channel ID or name where the target message lives. To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> channel;

    @Schema(
        title = "Emoji name",
        description = "Name without colons (e.g., thumbsup, white_check_mark, heart)."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> name;

    @Schema(
        title = "Message timestamp",
        description = "Slack `ts` of the message to remove the reaction from."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<Instant> timestamp;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ReactionsRemoveRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .name(runContext.render(this.name).as(String.class).orElseThrow())
            .timestamp(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElseThrow());

        call(runContext, (client) -> client.reactionsRemove(builder.build()));

        return null;
    }
}
