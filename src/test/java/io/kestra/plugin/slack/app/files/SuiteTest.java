package io.kestra.plugin.slack.app.files;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.*;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.EnabledIfSlackTokenSet;
import io.kestra.plugin.slack.app.AbstractSlackClientTest;
import io.kestra.plugin.slack.app.models.FileOutput;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@KestraTest
@EnabledIfSlackTokenSet
public class SuiteTest extends AbstractSlackClientTest {
    @Value("${slack.bot-token:}")
    private String botToken;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storage;

    private static String fileId = null;

    @Order(1)
    @Test
    void upload() throws Exception {
        String testContent = "This is a test file content";
        URI fileUri = storage.put(
            TenantService.MAIN_TENANT,
            null,
            new URI("/test-file.txt"),
            new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))
        );

        Upload task = Upload.builder()
            .id(IdUtils.create())
            .type(Upload.class.getName())
            .token(Property.ofValue(botToken))
            .from(Property.ofValue(fileUri.toString()))
            .filename(Property.ofValue("test-file.txt"))
            .title(Property.ofValue("Test File"))
            .channels(Property.ofValue(java.util.List.of("C0ACC6BT2GK")))
            .build();

        Upload.Output output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(output.getId()).isNotNull();
        fileId = output.getId();
    }

    @Order(2)
    @Test
    void info() throws Exception {
        Info task = Info.builder()
            .id(IdUtils.create())
            .type(Info.class.getName())
            .token(Property.ofValue(botToken))
            .fileId(Property.ofValue(fileId))
            .build();

        FileOutput output = task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));

        assertThat(output).isNotNull();
        assertThat(output.getId()).isEqualTo(fileId);
    }

    @Order(3)
    @Test
    void delete() throws Exception {
        Delete task = Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .token(Property.ofValue(botToken))
            .fileId(Property.ofValue(fileId))
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
    }
}
