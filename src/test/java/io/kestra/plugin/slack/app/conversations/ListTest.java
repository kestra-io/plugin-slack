package io.kestra.plugin.slack.app.conversations;

import java.io.InputStreamReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.io.CharStreams;
import com.slack.api.model.ConversationType;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class ListTest extends AbstractSlackClientTest {
    @Value("${slack.bot-token}")
    private String botToken;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        List task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversationslist"))
            .token(Property.ofValue("token"))
            .build();

        List.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(20L);
        assertThat(FakeWebhookController.data).contains("types=public_channel");
        assertThat(ionResult).contains("test1");
        assertThat(ionResult).contains("test19");
    }

    @Test
    void runWithFilters() throws Exception {
        List task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversationslist"))
            .token(Property.ofValue("token"))
            .types(Property.ofValue(java.util.List.of(ConversationType.PRIVATE_CHANNEL, ConversationType.PUBLIC_CHANNEL)))
            .excludeArchived(Property.ofValue(true))
            .build();

        List.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(20L);
        assertThat(FakeWebhookController.data).contains("exclude_archived=1");
        assertThat(FakeWebhookController.data).contains("types=private_channel%2Cpublic_channel");
        assertThat(ionResult).contains("test1");
        assertThat(ionResult).contains("test19");
    }

    @Test
    void runReal() throws Exception {
        List task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .token(Property.ofValue(botToken))
            .types(Property.ofValue(java.util.List.of(ConversationType.PRIVATE_CHANNEL, ConversationType.PUBLIC_CHANNEL)))
            .excludeArchived(Property.ofValue(true))
            .build();

        List.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isGreaterThan(1L);
        assertThat(ionResult).contains("help");
    }
}
