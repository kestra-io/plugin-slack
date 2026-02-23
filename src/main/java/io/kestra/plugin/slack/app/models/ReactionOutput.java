package io.kestra.plugin.slack.app.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class ReactionOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "Reaction name (emoji without colons)")
    String name;

    @Schema(title = "Reaction user count")
    Integer count;

    @Schema(title = "User IDs who reacted")
    List<String> users;

    @Schema(title = "Custom emoji URL")
    String url;

    public static ReactionOutput of(com.slack.api.model.Reaction reaction) {
        if (reaction == null) {
            return null;
        }

        return ReactionOutput.builder()
            .name(reaction.getName())
            .count(reaction.getCount())
            .users(reaction.getUsers())
            .url(reaction.getUrl())
            .build();
    }
}
