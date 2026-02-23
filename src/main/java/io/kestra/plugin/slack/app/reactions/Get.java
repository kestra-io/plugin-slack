package io.kestra.plugin.slack.app.reactions;

import com.slack.api.methods.request.reactions.ReactionsGetRequest;
import com.slack.api.methods.response.reactions.ReactionsGetResponse;
import io.kestra.core.models.annotations.Example;
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
    title = "Get Slack reactions for an item",
    description = "Fetches reactions on a message, file, or file comment and writes them to storage. Requires `reactions:read`."
)
@Plugin(
    examples = {
        @Example(
            title = "Get reactions for a message",
            full = true,
            code = """
                id: slack_get_reactions
                namespace: company.team

                tasks:
                  - id: get_reactions
                    type: io.kestra.plugin.slack.app.reactions.Get
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    timestamp: "{{ outputs.previous_task.timestamp }}"
                """
        ),
        @Example(
            title = "Get reactions and process them",
            full = true,
            code = """
                id: slack_process_reactions
                namespace: company.team

                tasks:
                  - id: post_message
                    type: io.kestra.plugin.slack.app.chats.Post
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    messageText: "React to this message!"

                  - id: get_reactions
                    type: io.kestra.plugin.slack.app.reactions.Get
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    timestamp: "{{ outputs.post_message.timestamp }}"

                  - id: log_reactions
                    type: io.kestra.plugin.core.log.Log
                    message: "Reactions file: {{ outputs.get_reactions.uri }}"
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
public class Get extends AbstractSlackClientConnection implements RunnableTask<Get.Output> {
    @Schema(
        title = "Channel containing the message",
        description = "Channel ID or name for the target message. To get the channel ID, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    protected Property<String> channel;

    @Schema(
        title = "File ID (optional)",
        description = "Use when retrieving reactions for a file."
    )
    protected Property<String> file;

    @Schema(
        title = "File comment ID (optional)",
        description = "Use when retrieving reactions for a file comment."
    )
    protected Property<String> fileComment;

    @Schema(
        title = "Message timestamp (optional)",
        description = "Slack `ts` of the message; required when targeting a message."
    )
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
        @Schema(title = "URI of stored reactions file")
        URI uri;
    }
}
