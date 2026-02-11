package io.kestra.plugin.slack.app.models;

import com.slack.api.model.canvas.CanvasDocumentSection;
import io.kestra.core.models.annotations.PluginProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@Schema(
    title = "Canvas section output information"
)
public class CanvasSectionOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "List of sections found in the canvas.")
    @PluginProperty
    List<CanvasDocumentSection> sections;

    public static CanvasSectionOutput of(List<CanvasDocumentSection> sections) {
        return CanvasSectionOutput.builder()
            .sections(sections)
            .build();
    }
}
