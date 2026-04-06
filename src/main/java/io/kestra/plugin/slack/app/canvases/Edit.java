package io.kestra.plugin.slack.app.canvases;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.methods.request.canvases.CanvasesEditRequest;
import com.slack.api.model.canvas.CanvasDocumentChange;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.slack.AbstractSlackClientConnection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Apply changes to a canvas",
    description = "Submits one or more Canvas API changes (insert, replace, delete, rename). Each change must include an operation plus either `documentContent` or `titleContent` depending on the operation."
)
@Plugin(
    examples = {
        @Example(
            title = "Insert content at the end of a canvas",
            full = true,
            code = """
                id: slack_edit_canvas
                namespace: company.team

                tasks:
                  - id: edit_canvas
                    type: io.kestra.plugin.slack.app.canvases.Edit
                    token: "{{ secret('SLACK_TOKEN') }}"
                    canvasId: "F1234567890"
                    changes:
                      - operation: "insert_at_end"
                        documentContent:
                          type: "markdown"
                          markdown: "## New Section\\n\\nThis is new content."
                """
        ),
        @Example(
            title = "Replace a section in a canvas",
            full = true,
            code = """
                id: slack_replace_canvas_section
                namespace: company.team

                tasks:
                  - id: replace_section
                    type: io.kestra.plugin.slack.app.canvases.Edit
                    token: "{{ secret('SLACK_TOKEN') }}"
                    canvasId: "F1234567890"
                    changes:
                      - operation: "replace"
                        sectionId: "temp:C:VXX8e648e6984e441c6aa8c61173"
                        documentContent:
                          type: "markdown"
                          markdown: "- [x] Task completed"
                """
        ),
        @Example(
            title = "Rename a canvas",
            full = true,
            code = """
                id: slack_rename_canvas
                namespace: company.team

                tasks:
                  - id: rename_canvas
                    type: io.kestra.plugin.slack.app.canvases.Edit
                    token: "{{ secret('SLACK_TOKEN') }}"
                    canvasId: "F1234567890"
                    changes:
                      - operation: "rename"
                        titleContent:
                          type: "markdown"
                          markdown: "Project Status :white_check_mark:"
                """
        )
    }
)
public class Edit extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    private static final ObjectMapper OBJECT_MAPPER = JacksonMapper.ofJson();

    @Schema(
        title = "Canvas ID",
        description = "Canvas to edit; find it in the canvas URL or from creation output."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> canvasId;

    @Schema(
        title = "Canvas change list",
        description = "Each entry must set `operation` plus `documentContent` or `titleContent`. Supported operations: insert_after, insert_before, insert_at_start, insert_at_end, replace, delete, rename."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<List<Map<String, Object>>> changes;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var builder = CanvasesEditRequest.builder();

        runContext.render(this.canvasId).as(String.class).ifPresent(builder::canvasId);

        var changesList = runContext.render(this.changes).asList(Map.class);
        if (!changesList.isEmpty()) {
            List<CanvasDocumentChange> documentChanges = changesList.stream()
                .map(change -> OBJECT_MAPPER.convertValue(change, CanvasDocumentChange.class))
                .collect(Collectors.toList());
            builder.changes(documentChanges);
        }

        call(runContext, (client) -> client.canvasesEdit(builder.build()));

        return null;
    }
}
