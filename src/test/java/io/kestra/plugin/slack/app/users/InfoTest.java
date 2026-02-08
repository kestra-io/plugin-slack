package io.kestra.plugin.slack.app.users;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.models.UserOutput;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class InfoTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Info task = Info.builder()
            .id(IdUtils.create())
            .type(Info.class.getName())
            .slack(this.client("users"))
            .token(Property.ofValue("token"))
            .user(Property.ofValue("U1234567890"))
            .build();

        UserOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(output.getId()).isEqualTo("U1234567890");
        assertThat(output.getName()).isEqualTo("johndoe");
        assertThat(output.getProfile()).isNotNull();
        assertThat(output.getProfile().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(FakeWebhookController.data).contains("user=U1234567890");
    }

    @Test
    void runWithLocale() throws Exception {
        Info task = Info.builder()
            .id(IdUtils.create())
            .type(Info.class.getName())
            .slack(this.client("users"))
            .token(Property.ofValue("token"))
            .user(Property.ofValue("U1234567890"))
            .includeLocale(Property.ofValue(true))
            .build();

        UserOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("user=U1234567890");
        assertThat(FakeWebhookController.data).contains("include_locale=1");
    }
}
