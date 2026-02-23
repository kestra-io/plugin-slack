package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsKickRequest;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Remove a user from a channel",
    description = """
        Removes a user without notifying them. Slack requires a user token (xoxp-); bot tokens cannot call this API. See Slack docs for [conversations.kick](https://docs.slack.dev/reference/methods/conversations.kick/) limits.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Remove a user from a channel",
            full = true,
            code = """
                id: slack_kick_user
                namespace: company.team

                tasks:
                  - id: kick_user
                    type: io.kestra.plugin.slack.app.conversations.Kick
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                    user: "U1234567890"
                """
        )
    }
)
public class Kick extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Channel ID",
        description = "Channel to remove the user from (Slack channel ID). To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "User ID to remove",
        description = "Member ID to remove (e.g., U123...); user must currently be in the channel. To get a user ID, go to the user's profile, click the three dots, and select 'Copy member ID'."
    )
    @NotNull
    private Property<String> user;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsKickRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.user).as(String.class).ifPresent(builder::user);

        call(runContext, (client) -> client.conversationsKick(builder.build()));

        return null;
    }
}
