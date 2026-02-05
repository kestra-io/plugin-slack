package io.kestra.plugin.slack.chats;


import com.slack.api.methods.request.chat.ChatUpdateRequest;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
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
    title = "Update a message in a channel."
)
public class Update extends AbstractSlackClientConnection implements RunnableTask<Update.Output>, MessagePayloadInterface, ChatInterface {
    private Property<String> payload;
    private Property<String> messageText;
    private Property<String> channel;
    @NotNull
    private Property<String> timestamp;
    private Property<String> username;
    private Property<String> iconUrl;
    private Property<String> iconEmoji;


    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatUpdateRequest.builder()
            .ts(runContext.render(this.timestamp).as(String.class).orElseThrow());

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
