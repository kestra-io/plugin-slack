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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Leave a Slack conversation (channel)."
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
                    type: io.kestra.plugin.slack.conversations.Leave
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    }
)
public class Leave extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "The ID of the channel to leave.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsLeaveRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);

        call(runContext, (client) -> client.conversationsLeave(builder.build()));

        return null;
    }
}
