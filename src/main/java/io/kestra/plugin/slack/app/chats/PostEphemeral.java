package io.kestra.plugin.slack.app.chats;


import com.slack.api.methods.request.chat.ChatPostEphemeralRequest;
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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Post an ephemeral message to a channel."
)
public class PostEphemeral extends AbstractSlackClientConnection implements RunnableTask<PostEphemeral.Output>, MessagePayloadInterface, ChatInterface {
    private Property<String> payload;
    private Property<String> messageText;
    private Property<String> channel;
    private Property<String> timestamp;
    private Property<String> username;
    private Property<String> iconUrl;
    private Property<String> iconEmoji;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatPostEphemeralRequest.builder();

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
            builder.threadTs(runContext.render(this.timestamp).as(String.class).orElseThrow());
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
