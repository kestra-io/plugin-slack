package io.kestra.plugin.slack.app.canvases;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class DeleteTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Delete task = Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .slack(this.client())
            .token(Property.ofValue("token"))
            .canvasId(Property.ofValue("F1234567890"))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("canvas_id=F1234567890");
    }
}
