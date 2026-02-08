package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatDeleteRequest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a message from a channel."
)
public class Delete extends AbstractSlackClientConnection implements RunnableTask<Delete.Output> {
    @Schema(title = "The channel ID where the message should be added.")
    @NotNull
    protected Property<String> channel;

    @Schema(title = "The timestamp of the message to remove.")
    @NotNull
    protected Property<Instant> timestamp;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatDeleteRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .ts(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElseThrow());

        var response = call(runContext, (client) -> client.chatDelete(builder.build()));

        return Delete.Output.builder()
            .timestamp(response.getTs())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the posted message.")
        @NotNull
        String timestamp;
    }
}
