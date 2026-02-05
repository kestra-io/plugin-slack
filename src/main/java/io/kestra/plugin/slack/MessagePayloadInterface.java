package io.kestra.plugin.slack;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;

public interface MessagePayloadInterface {
    @Schema(title = "Slack message payload")
    Property<String> getPayload();

    @Schema(
        title = "Message Text or JSON String",
        description = "The message content as a raw string. It can be plain text with markdown, or a JSON object. If not a valid JSON object, it is automatically wrapped in `{\"text\": \"...\"}`. This property is ignored if the `payload` property is set."
    )
    Property<String> getMessageText();
}
