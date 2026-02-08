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
    title = "Channel output information"
)
public class ConversationOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "The ID of the channel.")
    @NotNull
    @PluginProperty
    String id;

    @Schema(title = "The name of the channel.")
    @PluginProperty
    String name;

    @Schema(title = "Whether the channel is private.")
    @PluginProperty
    Boolean isPrivate;

    @Schema(title = "Whether the conversation is a channel.")
    @PluginProperty
    Boolean isChannel;

    @Schema(title = "Whether the conversation is a group.")
    @PluginProperty
    Boolean isGroup;

    @Schema(title = "Whether the conversation is an instant message.")
    @PluginProperty
    Boolean isInstantMessage;

    @Schema(title = "Whether the conversation is a multi-person instant message.")
    @PluginProperty
    Boolean isMultiPersonInstantMessage;

    @Schema(title = "The timestamp when the channel was created.")
    @PluginProperty
    Instant created;

    @Schema(title = "Whether the channel is archived.")
    @PluginProperty
    Boolean isArchived;

    @Schema(title = "Whether the channel is the general channel.")
    @PluginProperty
    Boolean isGeneral;

    @Schema(title = "The topic of the channel.")
    @PluginProperty
    ConversationTopicOutput topic;

    @Schema(title = "The purpose of the channel.")
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
