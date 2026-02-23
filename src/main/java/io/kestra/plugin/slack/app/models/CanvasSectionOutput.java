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
    title = "Canvas sections output"
)
public class CanvasSectionOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "Sections returned")
    @PluginProperty
    List<CanvasDocumentSection> sections;

    public static CanvasSectionOutput of(List<CanvasDocumentSection> sections) {
        return CanvasSectionOutput.builder()
            .sections(sections)
            .build();
    }
}
