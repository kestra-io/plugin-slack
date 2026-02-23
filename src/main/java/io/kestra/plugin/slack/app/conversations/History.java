package io.kestra.plugin.slack.app.conversations;

import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.MessageOutput;
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
    title = "Fetch Slack channel history",
    description = "Streams channel messages within optional time bounds into internal storage. Supports pagination and counts fetched messages."
)
@Plugin(
    examples = {
        @Example(
            title = "Fetch recent messages from a channel",
            full = true,
            code = """
                id: slack_fetch_history
                namespace: company.team

                tasks:
                  - id: fetch_history
                    type: io.kestra.plugin.slack.app.conversations.History
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    },
    metrics = {
        @Metric(
            name = "records",
            type = Counter.TYPE,
            description = "The number of messages fetched."
        )
    }
)
public class History extends AbstractSlackClientConnection implements RunnableTask<History.Output> {
    @Schema(
        title = "Channel ID",
        description = "Channel whose history to export; Slack channel ID. To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "Oldest timestamp",
        description = "Inclusive lower bound (Instant) converted to Slack ts."
    )
    private Property<Instant> oldest;

    @Schema(
        title = "Latest timestamp",
        description = "Inclusive upper bound (Instant) converted to Slack ts."
    )
    private Property<Instant> latest;

    @Schema(
        title = "Include threaded messages",
        description = "If true, returns thread messages as well; defaults to false."
    )
    @Builder.Default
    private Property<Boolean> inclusive = Property.ofValue(false);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ConversationsHistoryRequest.builder()
            .limit(200);

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        builder.inclusive(runContext.render(this.inclusive).as(Boolean.class).orElse(false));
        runContext.render(this.oldest).as(Instant.class).map(MessageService::toSlackTimestamp).ifPresent(builder::oldest);
        runContext.render(this.latest).as(Instant.class).map(MessageService::toSlackTimestamp).ifPresent(builder::latest);

        Long size = 0L;
        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            String cursor = null;
            do {
                builder.cursor(cursor);
                ConversationsHistoryResponse response = call(runContext, (client) -> client.conversationsHistory(builder.build()));

                size = size + response.getMessages().size();
                Flux<MessageOutput> flux = Flux.fromStream(response
                    .getMessages()
                    .stream()
                    .map(MessageOutput::of)
                );
                FileSerde.writeAll(fileWriter, flux).block();

                var newCursor = response.getResponseMetadata() != null && !response.getResponseMetadata().getNextCursor().isEmpty() ?
                    response.getResponseMetadata().getNextCursor() :
                    null;
                cursor = newCursor == null || newCursor.equals(cursor) ? null : newCursor;
            } while (cursor != null);
        }

        runContext.metric(Counter.of("records", size));

        return Output.builder()
            .size(size)
            .uri(runContext.storage().putFile(tempFile))
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "URI of stored messages file")
        URI uri;

        @Schema(title = "Number of messages fetched")
        Long size;
    }

}
