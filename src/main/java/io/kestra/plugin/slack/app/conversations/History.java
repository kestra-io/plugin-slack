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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Fetch conversation history from a Slack channel."
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
        title = "The ID of the channel.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "Start of time range of messages to include.",
        description = "Unix timestamp of the least recent message to include."
    )
    private Property<String> oldest;

    @Schema(
        title = "End of time range of messages to include.",
        description = "Unix timestamp of the most recent message to include."
    )
    private Property<String> latest;

    @Schema(
        title = "Include messages from all threads.",
        description = "Return all messages including those in threads. Default is false."
    )
    @Builder.Default
    private Property<Boolean> inclusive = Property.ofValue(false);

    @Schema(
        title = "The maximum number of messages to return per request.",
        description = "Maximum number of items to return per page. Recommended value is 200 or less. The default is 100."
    )
    private Property<Integer> limit;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ConversationsHistoryRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        builder.inclusive(runContext.render(this.inclusive).as(Boolean.class).orElse(false));
        runContext.render(this.oldest).as(String.class).ifPresent(builder::oldest);
        runContext.render(this.latest).as(String.class).ifPresent(builder::latest);
        runContext.render(this.limit).as(Integer.class).ifPresent(builder::limit);

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

                cursor = response.getResponseMetadata() != null ? response.getResponseMetadata().getNextCursor() : null;
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
        @Schema(title = "URI of the stored messages file")
        URI uri;

        @Schema(title = "The number of messages fetched")
        Long size;
    }

}
