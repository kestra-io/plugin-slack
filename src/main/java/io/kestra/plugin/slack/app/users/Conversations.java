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
    title = "List a user's Slack conversations",
    description = "Fetches channels the user belongs to, filtered by type and archived flag, and stores results to internal storage."
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
        title = "User ID (optional)",
        description = "User to query; defaults to the authenticated user."
    )
    private Property<String> user;

    @Schema(
        title = "Exclude archived channels",
        description = "If true, archived conversations are omitted. Default false."
    )
    @Builder.Default
    private Property<Boolean> excludeArchived = Property.ofValue(false);

    @Schema(
        title = "Conversation types",
        description = "List of types to include (public_channel, private_channel, mpim, im)."
    )
    @Builder.Default
    private Property<java.util.List<ConversationType>> types = Property.ofValue(java.util.List.of(ConversationType.PUBLIC_CHANNEL));

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = UsersConversationsRequest.builder()
            .limit(1000);

        runContext.render(this.user).as(String.class).ifPresent(builder::user);
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

                var newCursor = response.getResponseMetadata() != null && !response.getResponseMetadata().getNextCursor().isEmpty() ?
                    response.getResponseMetadata().getNextCursor() :
                    null;
                cursor = newCursor == null || newCursor.equals(cursor) ? null : newCursor;
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
        @Schema(title = "URI of stored conversations file")
        URI uri;

        @Schema(title = "Number of conversations fetched")
        Long size;
    }
}
