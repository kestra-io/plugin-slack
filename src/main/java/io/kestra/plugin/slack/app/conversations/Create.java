package io.kestra.plugin.slack.app.conversations;

import com.slack.api.methods.request.conversations.ConversationsCreateRequest;
import com.slack.api.methods.response.conversations.ConversationsCreateResponse;
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
    title = "Create a Slack channel",
    description = "Creates a public or private channel with the given name. Channel names must follow Slack limits."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a new public channel",
            full = true,
            code = """
                id: slack_create_channel
                namespace: company.team

                tasks:
                  - id: create_channel
                    type: io.kestra.plugin.slack.app.conversations.Create
                    token: "{{ secret('SLACK_TOKEN') }}"
                    name: "new-project-channel"
                    isPrivate: false
                """
        )
    }
)
public class Create extends AbstractSlackClientConnection implements RunnableTask<ConversationOutput> {
    @Schema(
        title = "Channel name",
        description = "Lowercase letters, numbers, hyphens, underscores only; max 80 characters."
    )
    @NotNull
    private Property<String> name;

    @Schema(
        title = "Create as private",
        description = "If true, creates a private channel; defaults to public."
    )
    @Builder.Default
    private Property<Boolean> isPrivate = Property.ofValue(false);

    @Schema(
        title = "Team ID",
        description = "Encoded team ID required when using an org token."
    )
    private Property<String> teamId;

    @Override
    public ConversationOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsCreateRequest.builder();

        runContext.render(this.name).as(String.class).ifPresent(builder::name);
        runContext.render(this.isPrivate).as(Boolean.class).ifPresent(builder::isPrivate);
        runContext.render(this.teamId).as(String.class).ifPresent(builder::teamId);

        ConversationsCreateResponse response = call(runContext, (client) -> client.conversationsCreate(builder.build()));

        return ConversationOutput.of(response.getChannel());
    }
}
