package io.kestra.plugin.slack.app.canvases;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.app.models.CanvasOutput;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class CreateTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Create task = Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .title(Property.ofValue("Project Documentation"))
            .documentContent(
                Property.ofValue(
                    Map.of(
                        "type", "markdown",
                        "markdown", "## Overview\n\nThis is a project documentation canvas."
                    )
                )
            )
            .build();

        CanvasOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("title");
        assertThat(output).isNotNull();
        assertThat(output.getCanvasId()).isNotNull();
    }
}
