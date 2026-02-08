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
    @Schema(title = "The reaction name (emoji name without colons).")
    String name;

    @Schema(title = "The number of users who reacted with this emoji.")
    Integer count;

    @Schema(title = "The list of user IDs who reacted with this emoji.")
    List<String> users;

    @Schema(title = "The URL of the custom emoji if applicable.")
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
