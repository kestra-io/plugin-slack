package io.kestra.plugin.slack.app.conversations;

import com.slack.api.methods.request.conversations.ConversationsCloseRequest;
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
    title = "Close a Slack conversation (Direct Message).",
    description = """
        This task closes a direct message or multi-person direct message.

        Note:
        Bot tokens (xoxb-...) cannot currently be used to close conversations.
        You must use a user token (xoxp-...) to close the conversation rather than a bot token.

        [See Slack documentation](https://docs.slack.dev/reference/methods/conversations.close/)
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Close a direct message",
            full = true,
            code = """
                id: slack_close_dm
                namespace: company.team

                tasks:
                  - id: close_dm
                    type: io.kestra.plugin.slack.app.conversations.Close
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "D1234567890"
                """
        )
    }
)
public class Close extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "The ID of the conversation to close.",
        description = "Conversation ID to close. This can be a direct message or multi-person direct message."
    )
    @NotNull
    private Property<String> channel;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsCloseRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);

        call(runContext, (client) -> client.conversationsClose(builder.build()));

        return null;
    }
}
