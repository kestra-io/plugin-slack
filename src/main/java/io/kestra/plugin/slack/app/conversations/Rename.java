package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsRenameRequest;
import com.slack.api.methods.response.conversations.ConversationsRenameResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.ConversationOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Rename a Slack channel",
    description = """
        Renames a channel. Requires a user token (xoxp-); Slack blocks bot tokens for this method. Follow Slack naming rules and limits; see Slack [conversations.rename](https://docs.slack.dev/reference/methods/conversations.rename).
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Rename a channel",
            full = true,
            code = """
                id: slack_rename_channel
                namespace: company.team

                tasks:
                  - id: rename_channel
                    type: io.kestra.plugin.slack.app.conversations.Rename
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                    name: "new-channel-name"
                """
        )
    }
)
public class Rename extends AbstractSlackClientConnection implements RunnableTask<ConversationOutput> {
    @Schema(
        title = "Channel ID",
        description = "Channel to rename; Slack channel ID required. To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "New channel name",
        description = "Lowercase letters, numbers, hyphens, and underscores only; max 80 characters."
    )
    @NotNull
    private Property<String> name;

    @Override
    public ConversationOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsRenameRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.name).as(String.class).ifPresent(builder::name);

        ConversationsRenameResponse response = call(runContext, (client) -> client.conversationsRename(builder.build()));

        return ConversationOutput.of(response.getChannel());
    }
}
