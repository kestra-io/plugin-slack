package io.kestra.plugin.slack;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import io.kestra.core.junit.annotations.KestraTest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KestraTest
public class AbstractSlackClientTest {
    @Inject
    protected EmbeddedServer embeddedServer;

    @Inject
    protected ApplicationContext applicationContext;

    @BeforeAll
    void startServer() {
        embeddedServer = applicationContext.getBean(EmbeddedServer.class);
        embeddedServer.start();
    }

    @AfterAll
    void stopServer() {
        if (embeddedServer != null) {
            embeddedServer.stop();
        }
    }

    @BeforeEach
    void reset() {
        FakeWebhookController.data = null;
    }

    protected Slack client() {
        SlackConfig slackConfig = new SlackConfig();
        slackConfig.setMethodsEndpointUrlPrefix(embeddedServer.getURI() + "/webhook-unit-test/mock/");
        slackConfig.setStatsEnabled(false);

        return Slack.getInstance(slackConfig);
    }
}