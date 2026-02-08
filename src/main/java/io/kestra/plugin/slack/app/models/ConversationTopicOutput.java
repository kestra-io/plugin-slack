package io.kestra.plugin.slack.app.models;

import com.slack.api.model.Purpose;
import com.slack.api.model.Topic;
import io.kestra.core.models.annotations.PluginProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@Schema(
    title = "Channel output information"
)
public class ConversationTopicOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "")
    @NotNull
    @PluginProperty
    String value;

    @Schema(title = "")
    @PluginProperty
    String creator;

    @Schema(title = "The timestamp when the channel was created.")
    @PluginProperty
    Integer lastSet;

    public static ConversationTopicOutput of(Topic topic) {
        if (topic == null) {
            return null;
        }

        return ConversationTopicOutput.builder()
            .value(topic.getValue())
            .creator(topic.getCreator())
            .lastSet(topic.getLastSet())
            .build();
    }

    public static ConversationTopicOutput of(Purpose purpose) {
        if (purpose == null) {
            return null;
        }

        return ConversationTopicOutput.builder()
            .value(purpose.getValue())
            .creator(purpose.getCreator())
            .lastSet(purpose.getLastSet())
            .build();
    }
}
