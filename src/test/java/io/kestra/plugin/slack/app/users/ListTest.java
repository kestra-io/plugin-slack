package io.kestra.plugin.slack.app.users;

import com.google.common.io.CharStreams;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.FakeWebhookController;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class ListTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        List task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .slack(this.client("userslist"))
            .token(Property.ofValue("token"))
            .build();

        List.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(15L);
        assertThat(output.getUri()).isNotNull();
        assertThat(ionResult).contains("U0000000000");
        assertThat(ionResult).contains("U0000000014");
    }
}
