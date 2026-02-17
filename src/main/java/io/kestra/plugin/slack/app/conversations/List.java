package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.ConversationOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "List Slack conversations (channels)."
)
@Plugin(
    examples = {
        @Example(
            title = "List all public channels",
            full = true,
            code = """
                id: slack_list_channels
                namespace: company.team

                tasks:
                  - id: list_channels
                    type: io.kestra.plugin.slack.app.conversations.List
                    token: "{{ secret('SLACK_TOKEN') }}"
                    types: ["public_channel"]
                """
        )
    },
    metrics = {
        @Metric(
            name = "records",
            type = Counter.TYPE,
            description = "The number of records fetch."
        )
    }
)
public class List extends AbstractSlackClientConnection implements RunnableTask<List.Output> {
    @Schema(
        title = "Mix and match channel types.",
        description = "Types include: public_channel, private_channel, mpim, im. Default is public_channel."
    )
    @Builder.Default
    private Property<java.util.List<ConversationType>> types = Property.ofValue(java.util.List.of(ConversationType.PUBLIC_CHANNEL));

    @Schema(
        title = "Exclude archived channels from the list.",
        description = "Default is false."
    )
    @Builder.Default
    private Property<Boolean> excludeArchived = Property.ofValue(false);

    @Schema(
        title = "Encoded team id.",
        description = "Required if org token is used."
    )
    private Property<String> teamId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ConversationsListRequest.builder()
            .limit(1000);

        builder.types(runContext
            .render(this.types)
            .asList(ConversationType.class)
        );
        builder.excludeArchived(runContext.render(this.excludeArchived).as(Boolean.class).orElse(false));
        runContext.render(this.teamId).as(String.class).ifPresent(builder::teamId);

        Long size = 0L;
        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            String cursor = null;
            do {
                builder.cursor(cursor);
                ConversationsListResponse response = call(
                    runContext,
                    (client) -> client.conversationsList(builder.build())
                );

                size = size + response.getChannels().size();
                Flux<ConversationOutput> flux = Flux.fromStream(response
                    .getChannels()
                    .stream()
                    .map(ConversationOutput::of)
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
        @Schema(title = "URI of the stored conversations file")
        URI uri;

        @Schema(title = "The size of the rows fetch")
        Long size;
    }
}
