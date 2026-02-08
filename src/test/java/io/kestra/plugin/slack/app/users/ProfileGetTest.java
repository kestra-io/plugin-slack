package io.kestra.plugin.slack.app.users;

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
public class ProfileGetTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        ProfileGet task = ProfileGet.builder()
            .id(IdUtils.create())
            .type(ProfileGet.class.getName())
            .slack(this.client("usersprofile"))
            .token(Property.ofValue("token"))
            .user(Property.ofValue("U1234567890"))
            .build();

        var output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(output.getTitle()).isEqualTo("Software Engineer");
        assertThat(output.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(FakeWebhookController.data).contains("user=U1234567890");
    }
}
