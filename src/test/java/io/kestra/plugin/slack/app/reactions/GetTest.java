package io.kestra.plugin.slack.app.reactions;

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
import io.kestra.plugin.slack.services.MessageService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class GetTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        Get task = Get.builder()
            .id(IdUtils.create())
            .type(Add.class.getName())
            .slack(this.client("reactionsget"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("@channel"))
            .timestamp(Property.ofValue(MessageService.fromSlackTimestamp("2023-01-01T00:00:00Z")))
            .build();

        Get.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(FakeWebhookController.data).isEqualTo("channel=%40channel&timestamp=1672531200&full=1");
        assertThat(ionResult).contains("thumbsup-0");
        assertThat(ionResult).contains("thumbsup-19");
    }
}
