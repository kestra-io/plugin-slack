package io.kestra.plugin.slack.app.models;

import com.slack.api.model.Conversation;
import io.kestra.core.models.annotations.PluginProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

import static io.kestra.plugin.slack.services.MessageService.fromSlackTimestamp;

@Value
@Builder
@Jacksonized
@Schema(
    title = "Conversation details output"
)
public class ConversationOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "Channel ID")
    @NotNull
    @PluginProperty
    String id;

    @Schema(title = "Channel name")
    @PluginProperty
    String name;

    @Schema(title = "Is private")
    @PluginProperty
    Boolean isPrivate;

    @Schema(title = "Is channel")
    @PluginProperty
    Boolean isChannel;

    @Schema(title = "Is group")
    @PluginProperty
    Boolean isGroup;

    @Schema(title = "Is instant message")
    @PluginProperty
    Boolean isInstantMessage;

    @Schema(title = "Is multi-person IM")
    @PluginProperty
    Boolean isMultiPersonInstantMessage;

    @Schema(title = "Created at")
    @PluginProperty
    Instant created;

    @Schema(title = "Is archived")
    @PluginProperty
    Boolean isArchived;

    @Schema(title = "Is general")
    @PluginProperty
    Boolean isGeneral;

    @Schema(title = "Channel topic")
    @PluginProperty
    ConversationTopicOutput topic;

    @Schema(title = "Channel purpose")
    @PluginProperty
    ConversationTopicOutput purpose;

    public static ConversationOutput of(Conversation channel) {
        return ConversationOutput.builder()
            .id(channel.getId())
            .name(channel.getName())
            .isChannel(channel.isChannel())
            .isGroup(channel.isGroup())
            .isInstantMessage(channel.isIm())
            .isMultiPersonInstantMessage(channel.isMpim())
            .isPrivate(channel.isPrivate())
            .created(fromSlackTimestamp(channel.getCreated()))
            .isArchived(channel.isArchived())
            .isGeneral(channel.isGeneral())
            .topic(ConversationTopicOutput.of(channel.getTopic()))
            .purpose(ConversationTopicOutput.of(channel.getPurpose()))
            .build();
    }
}
