package io.kestra.plugin.slack.app.conversations;

import com.slack.api.methods.request.conversations.ConversationsUnarchiveRequest;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Unarchive a Slack channel",
    description = """
        Restores an archived channel and adds the caller to it. Requires a user token (xoxp-); Slack does not allow bot tokens for this method. See [Slack conversations.unarchive documentation](https://docs.slack.dev/reference/methods/conversations.unarchive/) for details.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Unarchive a channel",
            full = true,
            code = """
                id: slack_unarchive_channel
                namespace: company.team

                tasks:
                  - id: unarchive_channel
                    type: io.kestra.plugin.slack.app.conversations.Unarchive
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    }
)
public class Unarchive extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Channel ID",
        description = "Channel to unarchive; must be provided as a Slack channel ID (e.g., C123...). To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> channel;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsUnarchiveRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);

        call(runContext, (client) -> client.conversationsUnarchive(builder.build()));

        return null;
    }
}
