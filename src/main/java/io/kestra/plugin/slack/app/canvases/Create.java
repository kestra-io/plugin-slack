package io.kestra.plugin.slack.app.canvases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.methods.request.canvases.CanvasesCreateRequest;
import com.slack.api.methods.response.canvases.CanvasesCreateResponse;
import com.slack.api.model.canvas.CanvasDocumentContent;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.CanvasOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a new Slack canvas."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a new canvas with markdown content",
            full = true,
            code = """
                id: slack_create_canvas
                namespace: company.team

                tasks:
                  - id: create_canvas
                    type: io.kestra.plugin.slack.app.canvases.Create
                    token: "{{ secret('SLACK_TOKEN') }}"
                    title: "Project Documentation"
                    documentContent:
                      type: "markdown"
                      markdown: |
                        ## Overview
                        
                        This is a project documentation canvas.
                        
                        - [ ] Task 1
                        - [ ] Task 2
                """
        )
    }
)
public class Create extends AbstractSlackClientConnection implements RunnableTask<CanvasOutput> {
    private static final ObjectMapper OBJECT_MAPPER = JacksonMapper.ofJson();

    @Schema(
        title = "Title of the canvas.",
        description = "The title text for the canvas."
    )
    @NotNull
    private Property<String> title;

    @Schema(
        title = "Document content of the canvas.",
        description = "A map containing 'type' (must be 'markdown') and 'markdown' with the canvas content."
    )
    @NotNull
    private Property<Map<String, String>> documentContent;

    @Override
    public CanvasOutput run(RunContext runContext) throws Exception {
        var builder = CanvasesCreateRequest.builder();

        runContext.render(this.title).as(String.class).ifPresent(builder::title);

        var docContentMap = runContext.render(this.documentContent).asMap(String.class, String.class);
        if (!docContentMap.isEmpty()) {
            CanvasDocumentContent docContent = OBJECT_MAPPER.convertValue(docContentMap, CanvasDocumentContent.class);
            builder.documentContent(docContent);
        }

        CanvasesCreateResponse response = call(runContext, (client) -> client.canvasesCreate(builder.build()));

        return CanvasOutput.of(response.getCanvasId());
    }
}
