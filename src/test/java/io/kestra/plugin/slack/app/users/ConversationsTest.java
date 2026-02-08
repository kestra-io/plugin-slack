package io.kestra.plugin.slack.app.users;

import com.google.common.io.CharStreams;
import com.slack.api.model.ConversationType;
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
public class ConversationsTest extends AbstractSlackClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        Conversations task = Conversations.builder()
            .id(IdUtils.create())
            .type(Conversations.class.getName())
            .slack(this.client("usersconversations"))
            .token(Property.ofValue("token"))
            .user(Property.ofValue("U1234567890"))
            .build();

        Conversations.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        String ionResult = CharStreams.toString(new InputStreamReader(storageInterface.get(TenantService.MAIN_TENANT, null, output.getUri())));

        assertThat(output).isNotNull();
        assertThat(output.getSize()).isEqualTo(10L);
        assertThat(ionResult).contains("C0000000000");
        assertThat(FakeWebhookController.data).contains("user=U1234567890");
    }

    @Test
    void runWithTypes() throws Exception {
        Conversations task = Conversations.builder()
            .id(IdUtils.create())
            .type(Conversations.class.getName())
            .slack(this.client("usersconversations"))
            .token(Property.ofValue("token"))
            .types(Property.ofValue(java.util.List.of(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL)))
            .build();

        Conversations.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(FakeWebhookController.data).contains("types=public_channel%2Cprivate_channel");
    }
}
