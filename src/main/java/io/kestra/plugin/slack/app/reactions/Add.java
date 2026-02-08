package io.kestra.plugin.slack.app.reactions;


import com.slack.api.methods.request.reactions.ReactionsAddRequest;
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
    title = "Add a reaction to a message."
)
public class Add extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(title = "Channel, private group, or IM channel to send message to.", description = "Can be an encoded ID, or a name.")
    @NotNull
    protected Property<String> channel;

    @Schema(title = "Reaction (emoji) name.")
    @NotNull
    protected Property<String> name;

    @Schema(title = "Timestamp of the message to add reaction to.")
    @NotNull
    protected Property<String> timestamp;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = ReactionsAddRequest.builder()
            .channel(runContext.render(this.channel).as(String.class).orElseThrow())
            .name(runContext.render(this.name).as(String.class).orElseThrow())
            .timestamp(runContext.render(this.timestamp).as(String.class).orElseThrow());

        call(runContext, (client) -> client.reactionsAdd(builder.build()));

        return null;
    }
}
