package io.kestra.plugin.slack.app.users;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.app.models.UserOutput;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class LookupByEmailTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        LookupByEmail task = LookupByEmail.builder()
            .id(IdUtils.create())
            .type(LookupByEmail.class.getName())
            .methodsEndpointUrlPrefix(this.client("users"))
            .token(Property.ofValue("token"))
            .email(Property.ofValue("john.doe@example.com"))
            .build();

        UserOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(output.getId()).isEqualTo("U1234567890");
        assertThat(output.getProfile()).isNotNull();
        assertThat(output.getProfile().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(FakeWebhookController.data).contains("email=john.doe%40example.com");
    }
}
