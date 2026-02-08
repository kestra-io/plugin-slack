package io.kestra.plugin.slack.app.users;

import com.slack.api.methods.request.users.UsersConversationsRequest;
import com.slack.api.methods.response.users.UsersConversationsResponse;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "List conversations the user is a member of."
)
@Plugin(
    examples = {
        @Example(
            title = "List all channels a user is in",
            full = true,
            code = """
                id: slack_user_conversations
                namespace: company.team

                tasks:
                  - id: get_user_channels
                    type: io.kestra.plugin.slack.app.users.Conversations
                    token: "{{ secret('SLACK_TOKEN') }}"
                    user: "U1234567890"
                """
        )
    },
    metrics = {
        @Metric(
            name = "records",
            type = Counter.TYPE,
            description = "The number of conversations fetched."
        )
    }
)
public class Conversations extends AbstractSlackClientConnection implements RunnableTask<Conversations.Output> {
    @Schema(
        title = "The user ID to get conversations for.",
        description = "If not provided, uses the authenticated user."
    )
    private Property<String> user;

    @Schema(
        title = "The maximum number of conversations to return per request.",
        description = "Default is 100, maximum is 1000."
    )
    private Property<Integer> limit;

    @Schema(
        title = "Exclude archived channels from the list.",
        description = "Default is false."
    )
    @Builder.Default
    private Property<Boolean> excludeArchived = Property.ofValue(false);

    @Schema(
        title = "Filter by conversation types.",
        description = "A comma-separated list of channel types (e.g., 'public_channel,private_channel')."
    )
    private Property<java.util.List<ConversationType>> types = Property.ofValue(java.util.List.of(ConversationType.PUBLIC_CHANNEL));

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = UsersConversationsRequest.builder();

        runContext.render(this.user).as(String.class).ifPresent(builder::user);
        runContext.render(this.limit).as(Integer.class).ifPresent(builder::limit);
        runContext.render(this.excludeArchived).as(Boolean.class).ifPresent(builder::excludeArchived);
        builder.types(runContext.render(this.types).asList(ConversationType.class));


        Long size = 0L;
        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            String cursor = null;
            do {
                builder.cursor(cursor);
                UsersConversationsResponse response = call(runContext, (client) -> client.usersConversations(builder.build()));

                size = size + response.getChannels().size();
                Flux<ConversationOutput> flux = Flux.fromStream(response
                    .getChannels()
                    .stream()
                    .map(ConversationOutput::of)
                );
                FileSerde.writeAll(fileWriter, flux).block();

                cursor = response.getResponseMetadata() != null ? response.getResponseMetadata().getNextCursor() : null;
            } while (cursor != null && !cursor.isEmpty());
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

        @Schema(title = "The number of conversations fetched")
        Long size;
    }
}
