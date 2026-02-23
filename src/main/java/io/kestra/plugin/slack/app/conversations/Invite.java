package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsInviteRequest;
import com.slack.api.methods.response.conversations.ConversationsInviteResponse;
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

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Invite users to a channel",
    description = "Adds one or more users to the specified channel. Requires channel ID and user IDs."
)
@Plugin(
    examples = {
        @Example(
            title = "Invite users to a channel",
            full = true,
            code = """
                id: slack_invite_users
                namespace: company.team

                tasks:
                  - id: invite_users
                    type: io.kestra.plugin.slack.app.conversations.Invite
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                    users: ["U1234567890", "U0987654321"]
                """
        )
    }
)
public class Invite extends AbstractSlackClientConnection implements RunnableTask<ConversationOutput> {
    @Schema(
        title = "Channel ID",
        description = "Channel to invite users to (Slack channel ID). To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "User IDs to invite",
        description = "List of Slack user IDs (U...) to add. To get a user ID, go to the user's profile, click the three dots, and select 'Copy member ID'."
    )
    @NotNull
    private Property<List<String>> users;

    @Override
    public ConversationOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsInviteRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        builder.users(runContext.render(this.users).asList(String.class));


        ConversationsInviteResponse response = call(runContext, (client) -> client.conversationsInvite(builder.build()));

        return ConversationOutput.of(response.getChannel());
    }
}
