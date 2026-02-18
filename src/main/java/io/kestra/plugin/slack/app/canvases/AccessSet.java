package io.kestra.plugin.slack.app.canvases;

import com.slack.api.methods.request.canvases.access.CanvasesAccessSetRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Set access level for a Slack canvas."
)
@Plugin(
    examples = {
        @Example(
            title = "Set canvas access to read-only for specific users",
            full = true,
            code = """
                id: slack_canvas_access
                namespace: company.team

                tasks:
                  - id: set_canvas_access
                    type: io.kestra.plugin.slack.app.canvases.AccessSet
                    token: "{{ secret('SLACK_TOKEN') }}"
                    canvasId: "F1234567890"
                    accessLevel: READ
                    userIds:
                      - "U1234567890"
                      - "U0987654321"
                """
        )
    }
)
public class AccessSet extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "The ID of the canvas.",
        description = "The canvas ID to set access for."
    )
    @NotNull
    private Property<String> canvasId;

    @Schema(
        title = "Access level to set.",
        description = "The access level for the canvas."
    )
    @NotNull
    private Property<AccessLevel> accessLevel;

    @Schema(
        title = "List of channel IDs to grant access to.",
        description = """
            List of channels you wish to update access for. Can only be used if `userIds` is not provided.
            """
    )
    private Property<List<String>> channelIds;

    @Schema(
        title = "List of user IDs to grant access to.",
        description = """
            List of users you wish to update access for. Can only be used if `channelIds` is not provided.
            """
    )
    private Property<List<String>> userIds;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = CanvasesAccessSetRequest.builder();

        runContext.render(this.canvasId).as(String.class).ifPresent(builder::canvasId);
        runContext.render(this.accessLevel).as(AccessLevel.class).ifPresent(level -> builder.accessLevel(level.getValue()));

        var rChannelIds = runContext.render(this.channelIds).asList(String.class);
        var rUserIds = runContext.render(this.userIds).asList(String.class);

        boolean hasChannels = rChannelIds != null && !rChannelIds.isEmpty();
        boolean hasUsers = rUserIds != null && !rUserIds.isEmpty();

        if (hasChannels == hasUsers) {
            throw new IllegalArgumentException("Exactly one of 'userIds' or 'channelIds' must be provided.");
        }

        if (hasChannels) {
            builder.channelIds(rChannelIds);
        } else {
            builder.userIds(rUserIds);
        }

        call(runContext, (client) -> client.canvasesAccessSet(builder.build()));

        return null;
    }

    @Getter
    public enum AccessLevel {
        READ("read"),
        WRITE("write");

        private final String value;

        AccessLevel(String value) {
            this.value = value;
        }
    }
}
