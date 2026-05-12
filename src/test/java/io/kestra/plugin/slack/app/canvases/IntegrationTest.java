package io.kestra.plugin.slack.app.canvases;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.EnabledIfSlackTokenSet;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.app.models.CanvasSectionOutput;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
@EnabledIfSlackTokenSet
public class IntegrationTest extends AbstractSlackClientTest {
    @Value("${slack.bot-token:}")
    private String botToken;

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        String random1 = IdUtils.create();
        String content1 = "# New Section " + random1 + "\n\nThis is new content from " + IdUtils.create() + ".";

        Edit task = Edit.builder()
            .id(IdUtils.create())
            .type(Edit.class.getName())
            .token(Property.ofValue(botToken))
            .canvasId(Property.ofValue("F0AE1KNAE07"))
            .changes(
                Property.ofValue(
                    List.of(
                        Map.of(
                            "operation", "replace",
                            "documentContent", Map.of(
                                "type", "markdown",
                                "markdown", content1
                            )
                        )
                    )
                )
            )
            .build();
        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        SectionsLookup lookup = SectionsLookup.builder()
            .id(IdUtils.create())
            .type(Edit.class.getName())
            .token(Property.ofValue(botToken))
            .canvasId(Property.ofValue("F0AE1KNAE07"))
            .criteria(
                Property.ofValue(
                    Map.of(
                        "sectionTypes", List.of("any_header"),
                        "containsText", random1
                    )
                )
            )
            .build();

        CanvasSectionOutput output = lookup.run(TestsUtils.mockRunContext(runContextFactory, lookup, Map.of()));

        String random2 = IdUtils.create();
        String content2 = "# New Section " + random1 + "\n\nThis is new content from " + IdUtils.create() + ".";

        task = Edit.builder()
            .id(IdUtils.create())
            .type(Edit.class.getName())
            .token(Property.ofValue(botToken))
            .canvasId(Property.ofValue("F0AE1KNAE07"))
            .changes(
                Property.ofValue(
                    List.of(
                        Map.of(
                            "operation", "insert_after",
                            "sectionId", output.getSections().getFirst().getId(),
                            "documentContent", Map.of(
                                "type", "markdown",
                                "markdown", content2
                            )
                        )
                    )
                )
            )
            .build();
        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        lookup = SectionsLookup.builder()
            .id(IdUtils.create())
            .type(Edit.class.getName())
            .token(Property.ofValue(botToken))
            .canvasId(Property.ofValue("F0AE1KNAE07"))
            .criteria(
                Property.ofValue(
                    Map.of(
                        "sectionTypes", List.of("any_header"),
                        "containsText", random2
                    )
                )
            )
            .build();

        assertThat(output.getSections()).hasSize(1);
        assertThat(output.getSections().getFirst().getId()).isNotNull();
    }
}
