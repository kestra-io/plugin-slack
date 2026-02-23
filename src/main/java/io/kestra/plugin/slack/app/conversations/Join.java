package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsJoinRequest;
import com.slack.api.methods.response.conversations.ConversationsJoinResponse;
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
    title = "Join a Slack channel",
    description = "Adds the caller to the specified channel. Fails if already a member or if the channel is archived."
)
@Plugin(
    examples = {
        @Example(
            title = "Join a public channel",
            full = true,
            code = """
                id: slack_join_channel
                namespace: company.team

                tasks:
                  - id: join_channel
                    type: io.kestra.plugin.slack.app.conversations.Join
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    }
)
public class Join extends AbstractSlackClientConnection implements RunnableTask<ConversationOutput> {
    @Schema(
        title = "Channel ID",
        description = "Channel to join; provide the Slack channel ID (e.g., C123...). To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Override
    public ConversationOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsJoinRequest.builder();
        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);

        ConversationsJoinResponse response = call(runContext, (client) -> client.conversationsJoin(builder.build()));

        return ConversationOutput.of(response.getChannel());
    }
}
