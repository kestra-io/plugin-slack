package io.kestra.plugin.slack.app.canvases;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.methods.request.canvases.sections.CanvasesSectionsLookupRequest;
import com.slack.api.methods.response.canvases.sections.CanvasesSectionsLookupResponse;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.CanvasSectionOutput;

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
    title = "Find sections within a canvas",
    description = "Searches a canvas for sections matching provided criteria (text, types, etc.) and returns matching sections."
)
@Plugin(
    examples = {
        @Example(
            title = "Find a section containing specific text",
            full = true,
            code = """
                id: slack_canvas_section_lookup
                namespace: company.team

                tasks:
                  - id: lookup_section
                    type: io.kestra.plugin.slack.app.canvases.SectionsLookup
                    token: "{{ secret('SLACK_TOKEN') }}"
                    canvasId: "F1234567890"
                    criteria:
                      containsText: "Project Status"
                """
        )
    }
)
public class SectionsLookup extends AbstractSlackClientConnection implements RunnableTask<CanvasSectionOutput> {
    private static final ObjectMapper OBJECT_MAPPER = JacksonMapper.ofJson();

    @Schema(
        title = "Canvas ID",
        description = "Canvas to search for matching sections."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> canvasId;

    @Schema(
        title = "Section search criteria",
        description = "Map converted to Slack `Criteria`; supports fields such as `containsText` or `sectionTypes`."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<Map<String, Object>> criteria;

    @Override
    public CanvasSectionOutput run(RunContext runContext) throws Exception {
        var builder = CanvasesSectionsLookupRequest.builder();

        runContext.render(this.canvasId).as(String.class).ifPresent(builder::canvasId);

        var criteriaMap = runContext.render(this.criteria).asMap(String.class, Object.class);
        CanvasesSectionsLookupRequest.Criteria sectionCriteria = OBJECT_MAPPER.convertValue(
            criteriaMap,
            CanvasesSectionsLookupRequest.Criteria.class
        );
        builder.criteria(sectionCriteria);

        CanvasesSectionsLookupResponse response = call(runContext, (client) -> client.canvasesSectionsLookup(builder.build()));

        return CanvasSectionOutput.of(response.getSections());
    }
}
