package io.kestra.plugin.slack.app.canvases;

import com.slack.api.methods.request.canvases.CanvasesDeleteRequest;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a canvas",
    description = "Permanently deletes the specified canvas; Slack cannot recover it after deletion."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a canvas",
            full = true,
            code = """
                id: slack_delete_canvas
                namespace: company.team

                tasks:
                  - id: delete_canvas
                    type: io.kestra.plugin.slack.app.canvases.Delete
                    token: "{{ secret('SLACK_TOKEN') }}"
                    canvasId: "F1234567890"
                """
        )
    }
)
public class Delete extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Canvas ID",
        description = "Canvas to delete; deletion is irreversible in Slack."
    )
    @NotNull
    private Property<String> canvasId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = CanvasesDeleteRequest.builder();

        runContext.render(this.canvasId).as(String.class).ifPresent(builder::canvasId);

        call(runContext, (client) -> client.canvasesDelete(builder.build()));

        return null;
    }
}
