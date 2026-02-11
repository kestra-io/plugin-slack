package io.kestra.plugin.slack.app.canvases;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.models.CanvasSectionOutput;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class SectionsLookupTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        SectionsLookup task = SectionsLookup.builder()
            .id(IdUtils.create())
            .type(SectionsLookup.class.getName())
            .slack(this.client())
            .token(Property.ofValue("token"))
            .canvasId(Property.ofValue("F1234567890"))
            .criteria(Property.ofValue(Map.of(
                "containsText", "Project Status"
            )))
            .build();

        CanvasSectionOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("canvas_id=F1234567890");
        assertThat(output).isNotNull();
    }
}
