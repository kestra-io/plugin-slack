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
    title = "List all users in a Slack workspace."
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
        title = "The maximum number of users to return per request.",
        description = "Maximum number of items to return per page. Default is 100, maximum is 1000."
    )
    private Property<Integer> limit;

    @Schema(
        title = "Include presence data for each user.",
        description = "Set to true to include presence data in the output. Default is false."
    )
    @Builder.Default
    private Property<Boolean> includePresence = Property.ofValue(false);

    @Schema(
        title = "Include locale for each user.",
        description = "Set to true to include locale information in the output. Default is false."
    )
    @Builder.Default
    private Property<Boolean> includeLocale = Property.ofValue(false);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = UsersListRequest.builder();

        runContext.render(this.limit).as(Integer.class).ifPresent(builder::limit);
        runContext.render(this.includePresence).as(Boolean.class).ifPresent(builder::presence);
        runContext.render(this.includeLocale).as(Boolean.class).ifPresent(builder::includeLocale);

        Long size = 0L;
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
        @Schema(title = "URI of the stored users file")
        URI uri;

        @Schema(title = "The number of users fetched")
        Long size;
    }
}
