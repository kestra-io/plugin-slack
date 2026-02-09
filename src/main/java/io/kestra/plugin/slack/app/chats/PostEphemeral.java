package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatPostEphemeralRequest;
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
    title = "Post an ephemeral message to a channel.",
    description = "Send an ephemeral (temporary) message that is only visible to a specific user in a channel. " +
        "Ephemeral messages disappear when the user navigates away. " +
        "You need the `chat:write` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Send an ephemeral message to a user",
            full = true,
            code = """
                id: slack_ephemeral_message
                namespace: company.team

                tasks:
                  - id: post_ephemeral
                    type: io.kestra.plugin.slack.app.chats.PostEphemeral
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    user: "U1234567890"
                    messageText: "This message is only visible to you"
                """
        ),
        @Example(
            title = "Send an ephemeral notification",
            full = true,
            code = """
                id: slack_ephemeral_notification
                namespace: company.team

                tasks:
                  - id: notify_user
                    type: io.kestra.plugin.slack.app.chats.PostEphemeral
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "#general"
                    user: "U1234567890"
                    payload: |
                      {
                        "text": "Task completed",
                        "blocks": [
                          {
                            "type": "section",
                            "text": {
                              "type": "mrkdwn",
                              "text": ":white_check_mark: Your task has been completed successfully"
                            }
                          }
                        ]
                      }
                """
        )
    }
)
public class PostEphemeral extends AbstractSlackClientConnection implements RunnableTask<PostEphemeral.Output>, MessagePayloadInterface, ChatInterface {
    @NotNull
    @Schema(
        title = "ID of the user who will receive the ephemeral message.",
        description = "The user should be in the channel specified by the channel argument. User IDs typically start with 'U' (e.g., U1234567890)."
    )
    private Property<String> user;

    private Property<String> payload;
    private Property<String> messageText;
    private Property<String> channel;
    private Property<Instant> timestamp;
    private Property<String> username;
    private Property<String> iconUrl;
    private Property<String> iconEmoji;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatPostEphemeralRequest.builder()
            .user(runContext.render(this.user).as(String.class).orElseThrow());

        String json = MessageService.prepareMessageAsJson(runContext, this.payload, this.messageText);
        Map<String, Object> map = JacksonMapper.toMap(json);

        if (map.containsKey("channel")) {
            builder.channel((String) map.get("channel"));
        } else if (this.channel != null) {
            builder.channel(runContext.render(this.channel).as(String.class).orElseThrow());
        }

        if (map.containsKey("thread_ts")) {
            builder.threadTs((String) map.get("thread_ts"));
        } else if (this.timestamp != null) {
            builder.threadTs(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElseThrow());
        }

        if (map.containsKey("icon_emoji")) {
            builder.iconEmoji((String) map.get("icon_emoji"));
        } else if (iconEmoji != null) {
            builder.iconEmoji(runContext.render(this.iconEmoji).as(String.class).orElseThrow());
        }

        if (map.containsKey("icon_url")) {
            builder.iconUrl((String) map.get("icon_url"));
        } else if (iconUrl != null) {
            builder.iconUrl(runContext.render(this.iconUrl).as(String.class).orElseThrow());
        }

        if (map.containsKey("username")) {
            builder.username((String) map.get("username"));
        } else if (username != null) {
            builder.username(runContext.render(this.username).as(String.class).orElseThrow());
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

        var response = call(runContext, (client) -> client.chatPostEphemeral(builder.build()));

        return PostEphemeral.Output.builder()
            .timestamp(response.getMessageTs())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The timestamp of the posted message.")
        @NotNull
        String timestamp;
    }
}
