package io.kestra.plugin.slack.app.conversations;

import com.slack.api.methods.request.conversations.ConversationsLeaveRequest;

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
    title = "Leave a Slack channel",
    description = "Removes the caller from the specified channel. Public channels can be rejoined; private channels may need an invite."
)
@Plugin(
    examples = {
        @Example(
            title = "Leave a channel",
            full = true,
            code = """
                id: slack_leave_channel
                namespace: company.team

                tasks:
                  - id: leave_channel
                    type: io.kestra.plugin.slack.app.conversations.Leave
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    }
)
public class Leave extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Channel ID",
        description = "Channel to leave; provide the Slack channel ID. To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> channel;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsLeaveRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);

        call(runContext, (client) -> client.conversationsLeave(builder.build()));

        return null;
    }
}
