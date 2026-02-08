package io.kestra.plugin.slack.app.chats;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public interface ChatInterface {
    @Schema(title = "Channel, group, or IM channel to send message to.", description = "Can be an encoded ID, or a name.")
    Property<String> getChannel();

    @Schema(title = "Provide another message's `timestamp` value to make this message a reply.")
    Property<Instant> getTimestamp();

    @Schema(title = "The username of the message.")
    Property<String> getUsername();

    @Schema(title = "URL to an image to use as the icon for this message.")
    Property<String> getIconUrl();

    @Schema(title = "Emoji to use as the icon for this message.", description = "Overrides `iconUrl`.")
    Property<String> getIconEmoji();

}
