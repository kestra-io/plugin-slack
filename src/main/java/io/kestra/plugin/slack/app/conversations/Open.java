package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.ConversationOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Open a DM or MPIM",
    description = "Opens a direct message with one user or a multi-person DM with several users. Can also resume an existing IM by ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Open a direct message with a user",
            full = true,
            code = """
                id: slack_open_dm
                namespace: company.team

                tasks:
                  - id: open_dm
                    type: io.kestra.plugin.slack.app.conversations.Open
                    token: "{{ secret('SLACK_TOKEN') }}"
                    users: ["U1234567890"]
                """
        ),
        @Example(
            title = "Open a multi-person direct message",
            full = true,
            code = """
                id: slack_open_mpim
                namespace: company.team

                tasks:
                  - id: open_mpim
                    type: io.kestra.plugin.slack.app.conversations.Open
                    token: "{{ secret('SLACK_TOKEN') }}"
                    users: ["U1234567890", "U0987654321", "U1122334455"]
                """
        )
    }
)
public class Open extends AbstractSlackClientConnection implements RunnableTask<ConversationOutput> {
    @Schema(
        title = "User IDs to include",
        description = "One ID opens a DM; multiple IDs open an MPIM. Provide Slack user IDs (U...). To get a user ID, go to the user's profile, click the three dots, and select 'Copy member ID'."
    )
    private Property<List<String>> users;

    @Schema(
        title = "Existing IM/MPIM ID",
        description = "Resume a conversation by ID instead of passing users."
    )
    private Property<String> channel;

    @Schema(
        title = "Reuse existing DM if present",
        description = "If true, return existing DM instead of creating a new one. Default false."
    )
    @Builder.Default
    private Property<Boolean> returnIm = Property.ofValue(false);

    @Override
    public ConversationOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsOpenRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.returnIm).as(Boolean.class).ifPresent(builder::returnIm);
        builder.users(runContext.render(this.users).asList(String.class));

        ConversationsOpenResponse response = call(runContext, (client) -> client.conversationsOpen(builder.build()));

        return ConversationOutput.of(response.getChannel());
    }
}
