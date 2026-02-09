package io.kestra.plugin.slack.app.conversations;

import com.slack.api.methods.request.conversations.ConversationsInfoRequest;
import com.slack.api.methods.response.conversations.ConversationsInfoResponse;
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
    title = "Get information about a Slack conversation (channel)."
)
@Plugin(
    examples = {
        @Example(
            title = "Get information about a channel",
            full = true,
            code = """
                id: slack_channel_info
                namespace: company.team

                tasks:
                  - id: get_channel_info
                    type: io.kestra.plugin.slack.app.conversations.Info
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    }
)
public class Info extends AbstractSlackClientConnection implements RunnableTask<ConversationOutput> {
    @Schema(
        title = "The ID of the channel.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "Include locale for this conversation.",
        description = "Set this to true to receive the locale for this conversation. Default is false."
    )
    @Builder.Default
    private Property<Boolean> includeLocale = Property.ofValue(false);

    @Override
    public ConversationOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsInfoRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.includeLocale).as(Boolean.class).ifPresent(builder::includeLocale);

        ConversationsInfoResponse response = call(runContext, (client) -> client.conversationsInfo(builder.build()));

        return ConversationOutput.of(response.getChannel());
    }
}
