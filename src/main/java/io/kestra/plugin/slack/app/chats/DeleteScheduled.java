package io.kestra.plugin.slack.app.chats;

import com.slack.api.methods.request.chat.ChatDeleteScheduledMessageRequest;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Cancel a scheduled Slack message",
    description = "Removes a pending scheduled message before it posts. Requires `chat:write`; provide the channel and scheduled message ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a scheduled message",
            full = true,
            code = """
                id: slack_delete_scheduled
                namespace: company.team

                tasks:
                  - id: delete_scheduled
                    type: io.kestra.plugin.slack.app.chats.DeleteScheduled
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    scheduledMessageId: "Q1234567890"
                """
        ),
        @Example(
            title = "Schedule and cancel a message",
            full = true,
            code = """
                id: slack_schedule_and_cancel
                namespace: company.team

                tasks:
                  - id: schedule_message
                    type: io.kestra.plugin.slack.app.chats.Schedule
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    messageText: "This will be cancelled"
                    postAt: "{{ now() | dateAdd(1, 'HOURS') }}"

                  - id: cancel_message
                    type: io.kestra.plugin.slack.app.chats.DeleteScheduled
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    scheduledMessageId: "{{ outputs.schedule_message.messageId }}"
                """
        )
    }
)
public class DeleteScheduled extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Channel of scheduled message",
        description = "Channel ID or name that holds the scheduled post."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> channel;

    @Schema(
        title = "Scheduled message ID",
        description = "Value returned by Schedule; uniquely identifies the pending post."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> scheduledMessageId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ChatDeleteScheduledMessageRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .scheduledMessageId(runContext.render(this.scheduledMessageId).as(String.class).orElseThrow());

        call(runContext, (client) -> client.chatDeleteScheduledMessage(builder.build()));

        return null;
    }
}
