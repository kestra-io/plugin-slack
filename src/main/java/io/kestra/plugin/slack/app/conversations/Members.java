package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsMembersRequest;
import com.slack.api.methods.response.conversations.ConversationsMembersResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
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
    title = "List members in a Slack conversation (channel)."
)
@Plugin(
    examples = {
        @Example(
            title = "List all members in a channel",
            full = true,
            code = """
                id: slack_list_members
                namespace: company.team

                tasks:
                  - id: list_members
                    type: io.kestra.plugin.slack.app.conversations.Members
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                """
        )
    },
    metrics = {
        @Metric(
            name = "records",
            type = Counter.TYPE,
            description = "The number of members fetched."
        )
    }
)
public class Members extends AbstractSlackClientConnection implements RunnableTask<Members.Output> {
    @Schema(
        title = "The ID of the channel.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ConversationsMembersRequest.builder()
            .limit(1000);

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);

        Long size = 0L;
        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            String cursor = null;
            do {
                builder.cursor(cursor);
                ConversationsMembersResponse response = call(runContext, (client) -> client.conversationsMembers(builder.build()));

                size = size + response.getMembers().size();
                Flux<MemberOutput> flux = Flux.fromStream(response
                    .getMembers()
                    .stream()
                    .map(memberId -> MemberOutput.builder().memberId(memberId).build())
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
        @Schema(title = "URI of the stored members file")
        URI uri;

        @Schema(title = "The number of members fetched")
        Long size;
    }

    @Value
    @Builder
    @Jacksonized
    public static class MemberOutput {
        @Schema(title = "The member user ID")
        String memberId;
    }
}
