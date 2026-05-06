package io.kestra.plugin.slack.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.slack.AbstractSlackWebhookConnection;
import io.kestra.plugin.slack.FakeWebhookController;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;

@KestraTest
class SlackIncomingWebhookProxyTest {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void runWithProxy() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());
        EmbeddedServer embeddedServer = applicationContext.getBean(EmbeddedServer.class);
        embeddedServer.start();

        String serverUrl = embeddedServer.getURI() + "/webhook-unit-test";
        int serverPort = embeddedServer.getPort();

        SlackIncomingWebhook failingTask = SlackIncomingWebhook.builder()
            .id("test-proxy-fail")
            .type(SlackIncomingWebhook.class.getName())
            .url(serverUrl)
            .messageText(Property.ofValue("Hello through proxy"))
            .options(AbstractSlackWebhookConnection.RequestOptions.builder()
                .proxy(AbstractSlackWebhookConnection.ProxyOptions.builder()
                    .type(Property.ofValue(AbstractSlackWebhookConnection.ProxyType.HTTP))
                    .address(Property.ofValue("localhost"))
                    .port(Property.ofValue(1))
                    .build())
                .build())
            .build();

        Exception exception = assertThrows(Exception.class, () -> failingTask.run(runContext));
        assertThat(exception).isNotNull();

        SlackIncomingWebhook successTask = SlackIncomingWebhook.builder()
            .id("test-proxy-success")
            .type(SlackIncomingWebhook.class.getName())
            .url(serverUrl)
            .messageText(Property.ofValue("Hello through proxy"))
            .options(AbstractSlackWebhookConnection.RequestOptions.builder()
                .proxy(AbstractSlackWebhookConnection.ProxyOptions.builder()
                    .type(Property.ofValue(AbstractSlackWebhookConnection.ProxyType.HTTP))
                    .address(Property.ofValue("localhost"))
                    .port(Property.ofValue(serverPort))
                    .build())
                .build())
            .build();

        successTask.run(runContext);
        assertThat(FakeWebhookController.data).contains("Hello through proxy");
    }
}
