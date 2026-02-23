package io.kestra.plugin.slack.app.chats;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public interface ChatInterface {
    @Schema(title = "Channel to send message", description = "Channel ID or name; can also be set inside the payload.")
    Property<String> getChannel();

    @Schema(title = "Thread timestamp to reply to", description = "Slack `ts` value for replying in a thread; must belong to the same channel.")
    Property<Instant> getTimestamp();

    @Schema(title = "Display username", description = "Overrides the bot's default display name for this message.")
    Property<String> getUsername();

    @Schema(title = "Message icon URL", description = "HTTPS image used as icon when `iconEmoji` is not provided.")
    Property<String> getIconUrl();

    @Schema(title = "Message icon emoji", description = "Emoji code (e.g., :wave:) that overrides `iconUrl`.")
    Property<String> getIconEmoji();

}
