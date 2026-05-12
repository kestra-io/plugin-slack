package io.kestra.plugin.slack.app.conversations;

import java.io.InputStreamReader;
import java.time.Instant;
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
import io.kestra.plugin.slack.EnabledIfSlackTokenSet;
import io.kestra.plugin.slack.FakeWebhookController;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@KestraTest
public class HistoryTest extends AbstractSlackClientTest {
    @Value("${slack.bot-token:}")
    private String botToken;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        History task = History.builder()
            .id(IdUtils.create())
            .type(History.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversationshistory"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("C1234567890"))
            .build();

        History.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(30L);
        assertThat(FakeWebhookController.data).contains("channel=C1234567890");
        assertThat(ionResult).contains("Message 0");
        assertThat(ionResult).contains("Message 29");
        assertThat(ionResult).contains("U0000000000");
    }

    @Test
    void runWithFilters() throws Exception {
        History task = History.builder()
            .id(IdUtils.create())
            .type(History.class.getName())
            .methodsEndpointUrlPrefix(this.client("conversationshistory"))
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("C1234567890"))
            .oldest(Property.ofValue(Instant.ofEpochSecond(1609459200)))
            .latest(Property.ofValue(Instant.ofEpochSecond(1612137600)))
            .inclusive(Property.ofValue(true))
            .build();

        History.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(30L);
        assertThat(FakeWebhookController.data).contains("channel=C1234567890");
        assertThat(FakeWebhookController.data).contains("oldest=1609459200");
        assertThat(FakeWebhookController.data).contains("latest=1612137600");
        assertThat(FakeWebhookController.data).contains("inclusive=1");
        assertThat(ionResult).contains("Message 0");
        assertThat(ionResult).contains("Message 29");
    }

    @Test
    @EnabledIfSlackTokenSet
    void runIntegration() throws Exception {
        History task = History.builder()
            .id(IdUtils.create())
            .type(History.class.getName())
            .token(Property.ofValue(botToken))
            .channel(Property.ofValue("C0ACC6BT2GK"))
            .build();

        History.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isGreaterThan(1L);
        assertThat(ionResult).contains("test-file.txt");
        assertThat(ionResult).contains("<@U0ACC3PMNFM> test 2");
    }
}
