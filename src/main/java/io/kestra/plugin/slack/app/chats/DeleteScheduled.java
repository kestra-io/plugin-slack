package io.kestra.plugin.slack.app.chats;

import com.slack.api.methods.request.chat.ChatDeleteScheduledMessageRequest;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a scheduled message from a channel."
)
public class DeleteScheduled extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(title = "The channel ID where the scheduled message should be removed.")
    @NotNull
    protected Property<String> channel;

    @Schema(title = "The scheduled message ID to delete.")
    @NotNull
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
