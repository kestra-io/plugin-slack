package io.kestra.plugin.slack.app.users;

import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.users.UsersListResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.UserOutput;
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
    title = "List Slack workspace users",
    description = "Retrieves users with pagination and writes them to internal storage; emits record count."
)
@Plugin(
    examples = {
        @Example(
            title = "List all users in the workspace",
            full = true,
            code = """
                id: slack_list_users
                namespace: company.team

                tasks:
                  - id: list_users
                    type: io.kestra.plugin.slack.app.users.List
                    token: "{{ secret('SLACK_TOKEN') }}"
                """
        )
    },
    metrics = {
        @Metric(
            name = "records",
            type = Counter.TYPE,
            description = "The number of users fetched."
        )
    }
)
public class List extends AbstractSlackClientConnection implements RunnableTask<List.Output> {
    @Schema(
        title = "Include locale",
        description = "If true, locale fields are included. Default false."
    )
    @Builder.Default
    private Property<Boolean> includeLocale = Property.ofValue(false);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = UsersListRequest.builder()
            .limit(1000);

        runContext.render(this.includeLocale).as(Boolean.class).ifPresent(builder::includeLocale);

        long size = 0L;
        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            String cursor = null;
            do {
                builder.cursor(cursor);
                UsersListResponse response = call(runContext, (client) -> client.usersList(builder.build()));

                size = size + response.getMembers().size();
                Flux<UserOutput> flux = Flux.fromStream(response
                    .getMembers()
                    .stream()
                    .map(UserOutput::of)
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
        @Schema(title = "URI of stored users file")
        URI uri;

        @Schema(title = "Number of users fetched")
        Long size;
    }
}
