package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatUpdateRequest;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.MessagePayloadInterface;
import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Update a message in a channel.",
    description = "Update an existing message in a Slack channel. The message content and formatting can be changed. " +
        "You need the `chat:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Update a message",
            full = true,
            code = """
                id: slack_update_message
                namespace: company.team

                tasks:
                  - id: post_message
                    type: io.kestra.plugin.slack.app.chats.Post
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    messageText: "Processing..."

                  - id: update_message
                    type: io.kestra.plugin.slack.app.chats.Update
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    timestamp: "{{ outputs.post_message.timestamp }}"
                    messageText: "Processing complete!"
                """
        ),
        @Example(
            title = "Update with custom blocks",
            full = true,
            code = """
                id: slack_update_custom
                namespace: company.team

                tasks:
                  - id: update_message
                    type: io.kestra.plugin.slack.app.chats.Update
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    timestamp: "{{ outputs.previous_task.timestamp }}"
                    payload: |
                      {
                        "text": "Updated status",
                        "blocks": [
                          {
                            "type": "section",
                            "text": {
                              "type": "mrkdwn",
                              "text": "*Status:* :white_check_mark: Completed"
                            }
                          }
                        ]
                      }
                """
        )
    }
)
public class Update extends AbstractSlackClientConnection implements RunnableTask<Update.Output>, MessagePayloadInterface, ChatInterface {
    private Property<String> payload;
    private Property<String> messageText;
    private Property<String> channel;
    @NotNull
    private Property<Instant> timestamp;
    private Property<String> username;
    private Property<String> iconUrl;
    private Property<String> iconEmoji;


    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatUpdateRequest.builder()
            .ts(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElseThrow());

        String json = MessageService.prepareMessageAsJson(runContext, this.payload, this.messageText);
        Map<String, Object> map = JacksonMapper.toMap(json);

        if (map.containsKey("channel")) {
            builder.channel((String) map.get("channel"));
        } else if (this.channel != null) {
            builder.channel(runContext.render(this.channel).as(String.class).orElseThrow());
        }

        if (map.containsKey("text")) {
            builder.text((String) map.get("text"));
        }

        if (map.containsKey("blocks")) {
            builder.blocksAsString(JacksonMapper.ofJson().writeValueAsString(map.get("blocks")));
        }

        if (map.containsKey("attachments")) {
            builder.attachmentsAsString(JacksonMapper.ofJson().writeValueAsString(map.get("attachments")));
        }

        if (map.containsKey("reply_broadcast")) {
            builder.replyBroadcast((Boolean) map.get("reply_broadcast"));
        }

        if (map.containsKey("metadata")) {
            builder.metadataAsString(JacksonMapper.ofJson().writeValueAsString(map.get("metadata")));
        }

        ChatUpdateResponse response = call(runContext, (client) -> client.chatUpdate(builder.build()));

        return Update.Output.builder()
            .timestamp(response.getTs())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the updated message.")
        @NotNull
        String timestamp;
    }
}
