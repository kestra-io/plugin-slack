package io.kestra.plugin.slack.app.chats;

import com.slack.api.methods.request.chat.ChatScheduleMessageRequest;
import com.slack.api.methods.response.chat.ChatScheduleMessageResponse;
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
    title = "Schedule a message to be sent to a channel at a specified time."
)
public class Schedule extends AbstractSlackClientConnection implements RunnableTask<Schedule.Output>, MessagePayloadInterface, ChatInterface {
    private Property<String> payload;
    private Property<String> messageText;
    private Property<String> channel;
    private Property<String> timestamp;
    private Property<String> username;
    private Property<String> iconUrl;
    private Property<String> iconEmoji;

    @Schema(title = "Unix EPOCH timestamp of time in future to send the message.")
    @NotNull
    protected Property<Integer> postAt;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = ChatScheduleMessageRequest.builder()
            .postAt(runContext.render(this.postAt).as(Integer.class).orElseThrow());

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

        if (map.containsKey("unfurl_links")) {
            builder.unfurlLinks((Boolean) map.get("unfurl_links"));
        }

        if (map.containsKey("unfurl_media")) {
            builder.unfurlMedia((Boolean) map.get("unfurl_media"));
        }

        if (map.containsKey("metadata")) {
            builder.metadataAsString(JacksonMapper.ofJson().writeValueAsString(map.get("metadata")));
        }

        ChatScheduleMessageResponse response = call(runContext, (client) -> client.chatScheduleMessage(builder.build()));

        return Schedule.Output.builder()
            .messageId(response.getScheduledMessageId())
            .postAt(response.getPostAt())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the scheduled message.")
        @NotNull
        String messageId;

        @Schema(title = "The Unix timestamp when the message will be posted.")
        @NotNull
        Integer postAt;
    }
}
