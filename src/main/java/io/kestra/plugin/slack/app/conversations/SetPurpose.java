package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsSetPurposeRequest;
import com.slack.api.methods.response.conversations.ConversationsSetPurposeResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.ConversationTopicOutput;
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
    title = "Set the purpose of a Slack conversation (channel)."
)
@Plugin(
    examples = {
        @Example(
            title = "Set a channel purpose",
            full = true,
            code = """
                id: slack_set_purpose
                namespace: company.team

                tasks:
                  - id: set_purpose
                    type: io.kestra.plugin.slack.app.conversations.SetPurpose
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                    purpose: "This channel is for discussing the new project"
                """
        )
    }
)
public class SetPurpose extends AbstractSlackClientConnection implements RunnableTask<ConversationTopicOutput> {
    @Schema(
        title = "The ID of the channel.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "The new purpose for the channel.",
        description = "The purpose can be up to 250 characters."
    )
    @NotNull
    private Property<String> purpose;

    @Override
    public ConversationTopicOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsSetPurposeRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.purpose).as(String.class).ifPresent(builder::purpose);

        ConversationsSetPurposeResponse response = call(runContext, (client) -> client.conversationsSetPurpose(builder.build()));

        return ConversationTopicOutput.of(response.getChannel().getPurpose());
    }
}
