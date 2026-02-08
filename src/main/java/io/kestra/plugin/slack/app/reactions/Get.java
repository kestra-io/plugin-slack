package io.kestra.plugin.slack.app.reactions;

import com.slack.api.methods.request.reactions.ReactionsGetRequest;
import com.slack.api.methods.response.reactions.ReactionsGetResponse;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.ReactionOutput;
import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.time.Instant;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Get reactions for an item."
)
@Plugin(
    metrics = {
        @Metric(
            name = "records",
            type = Counter.TYPE,
            description = "The number of records fetch."
        )
    }
)
public class Get extends AbstractSlackClientConnection implements RunnableTask<Get.Output> {
    @Schema(title = "Channel, private group, or IM channel to send message to.", description = "Can be an encoded ID, or a name.")
    @NotNull
    protected Property<String> channel;

    @Schema(title = "File to get reactions for.")
    protected Property<String> file;

    @Schema(title = "File comment to get reactions for.")
    protected Property<String> fileComment;

    @Schema(title = "Timestamp of the message to add reaction to.")
    protected Property<Instant> timestamp;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ReactionsGetRequest.builder()
            .full(true);

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.file).as(String.class).ifPresent(builder::file);
        runContext.render(this.fileComment).as(String.class).ifPresent(builder::fileComment);
        runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).ifPresent(builder::timestamp);

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            ReactionsGetResponse response = call(runContext, (client) -> client.reactionsGet(builder.build()));

            Flux<ReactionOutput> flux = Flux.fromStream(response
                .getMessage()
                .getReactions()
                .stream()
                .map(ReactionOutput::of)
            );
            FileSerde.writeAll(fileWriter, flux).block();
            runContext.metric(Counter.of("records", response.getMessage().getReactions().size()));

        }

        return Output.builder()
            .uri(runContext.storage().putFile(tempFile))
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "URI of the stored conversations file")
        URI uri;
    }
}
