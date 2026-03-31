package io.kestra.plugin.slack.app.canvases;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class AccessSetTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        AccessSet task = AccessSet.builder()
            .id(IdUtils.create())
            .type(AccessSet.class.getName())
            .methodsEndpointUrlPrefix(this.client())
            .token(Property.ofValue("token"))
            .canvasId(Property.ofValue("F1234567890"))
            .accessLevel(Property.ofValue(AccessSet.AccessLevel.READ))
            .userIds(Property.ofValue(List.of("U1234567890", "U0987654321")))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(FakeWebhookController.data).contains("canvas_id=F1234567890");
        assertThat(FakeWebhookController.data).contains("access_level=read");
    }
}
