package io.kestra.plugin.slack.app;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.slack.api.SlackConfig;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.slack.FakeWebhookController;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KestraTest
public class AbstractSlackClientTest {
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final ObjectMapper MAPPER = JacksonMapper.ofJson().copy().setPropertyNamingStrategy(
        PropertyNamingStrategies.SNAKE_CASE
    );

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

    protected String client() {
        return this.client(null);
    }

    protected String client(String path) {
        return embeddedServer.getURI() + "/webhook-unit-test/mock/" + (path != null ? path + "/" : "");
    }

    public static Map<String, Object> convertToSlack(Object obj) {
        return MAPPER.convertValue(obj, MAP_TYPE_REFERENCE);
    }
}