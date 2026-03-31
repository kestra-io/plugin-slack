package io.kestra.plugin.slack.app.conversations;

import java.io.InputStreamReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.io.CharStreams;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class MembersTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        Members task = Members.builder()
            .id(IdUtils.create())
            .type(Members.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversationsmembers"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("C1234567890"))
            .build();

        Members.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(25L);
        assertThat(FakeWebhookController.data).contains("channel=C1234567890");
        assertThat(ionResult).contains("U0000000001");
        assertThat(ionResult).contains("U0000000024");
    }
}
