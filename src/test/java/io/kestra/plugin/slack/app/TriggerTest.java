package io.kestra.plugin.slack.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.slack.api.app_backend.SlackSignature;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.property.Property;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.services.WebhookService;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.core.utils.UriProvider;
import io.kestra.plugin.core.trigger.AbstractWebhookTrigger;
import io.kestra.plugin.core.trigger.WebhookContext;
import io.kestra.plugin.core.trigger.WebhookResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class TriggerTest {
    private final String signingSecret = "foo-bar-baz";
    private final SlackSignature.Generator generator = new SlackSignature.Generator(signingSecret);

    @Inject
    private UriProvider uriProvider;

    @Inject
    private WebhookService webhookService;

    private Flow flow(AbstractWebhookTrigger trigger) {
        return TestsUtils
            .mockFlow()
            .toBuilder()
            .triggers(List.of(trigger))
            .build();
    }

    private WebhookContext webhookContext(Flow flow, Trigger appTrigger, String body) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        var headers =  HttpHeaders.of(Map.of(
            "Content-Type", List.of("application/json"),
            SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP, List.of(timestamp),
            SlackSignature.HeaderNames.X_SLACK_SIGNATURE, List.of(generator.generate(timestamp, body))
        ), (s1, s2) -> true);

        HttpRequest post = HttpRequest.builder()
            .uri(uriProvider.webhookUrl(flow, appTrigger))
            .method("POST")
            .headers(headers)
            .body(HttpRequest.StringRequestBody.of(body))
            .build();

        WebhookContext build = WebhookContext.builder()
            .request(post)
            .flow(flow)
            .trigger(appTrigger)
            .webhookService(webhookService)
            .build();

        return build;
    }

    private static void assertResponse(HttpResponse<?> evaluate) throws JsonProcessingException {
        assertThat(evaluate.getStatus().getCode(), is(200));
        assertThat(evaluate.contentType(), is("application/json; charset=utf-8"));
        WebhookResponse webhookResponse = JacksonMapper.ofJson().readValue((String) evaluate.getBody(), WebhookResponse.class);
        assertThat(webhookResponse.id(), notNullValue());
    }

    @Test
    void challenge() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = JacksonMapper.ofJson()
            .writeValueAsString(Map.of(
                "token", "fixed-value",
                "challenge", "challenge-value",
                "type", "url_verification"
            ));

        WebhookContext build = webhookContext(flow, appTrigger, body);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertThat(evaluate.getStatus().getCode(), is(200));
        assertThat(evaluate.contentType(), is("text/plain"));
        assertThat(evaluate.getBody(), is("challenge-value"));
    }

    @Test
    void command() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "token=gIkuvaNzQIHg97ATvDxqgjtO" +
            "&team_id=T0001" +
            "&team_domain=example" +
            "&enterprise_id=E0001" +
            "&enterprise_name=Globular%20Construct%20Inc" +
            "&channel_id=C2147483705" +
            "&channel_name=test" +
            "&user_id=U2147483697" +
            "&user_name=Steve" +
            "&command=/weather-123" +
            "&text=94070" +
            "&response_url=https://hooks.slack.com/commands/1234/5678" +
            "&trigger_id=13345224609.738474920.8088930838d88f008e0";

        WebhookContext build = webhookContext(flow, appTrigger, body);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void events() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"token\":\"legacy-fixed-value\",\"team_id\":\"T123\",\"api_app_id\":\"A123\",\"event\":{\"client_msg_id\":\"3fd13273-5a6a-4b5c-bd6f-109fd697038c\",\"type\":\"message\",\"text\":\"<@U123> test\",\"user\":\"U234\",\"ts\":\"1583636399.000700\",\"team\":\"T123\",\"blocks\":[{\"type\":\"rich_text\",\"block_id\":\"FMAzp\",\"elements\":[{\"type\":\"rich_text_section\",\"elements\":[{\"type\":\"user\",\"user_id\":\"U123\"},{\"type\":\"text\",\"text\":\" test\"}]}]}],\"channel\":\"C123\",\"event_ts\":\"1583636399.000700\",\"channel_type\":\"channel\"},\"type\":\"event_callback\",\"event_id\":\"EvV1KA7U3A\",\"event_time\":1583636399,\"authed_users\":[\"U123\"]}";

        WebhookContext build = webhookContext(flow, appTrigger, body);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void blockActions() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"block_actions\",\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"container\":{\"type\":\"message\",\"message_ts\":\"1234567890.123456\",\"channel_id\":\"C123\",\"is_ephemeral\":false},\"trigger_id\":\"trigger-id-123\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"enterprise\":null,\"is_enterprise_install\":false,\"channel\":{\"id\":\"C123\",\"name\":\"general\"},\"message\":{\"bot_id\":\"B123\",\"type\":\"message\",\"text\":\"Click the button\",\"user\":\"U123\",\"ts\":\"1234567890.123456\"},\"state\":{\"values\":{}},\"response_url\":\"https://hooks.slack.com/actions/T123/123/abc\",\"actions\":[{\"action_id\":\"button_click\",\"block_id\":\"block123\",\"text\":{\"type\":\"plain_text\",\"text\":\"Click Me\",\"emoji\":true},\"value\":\"button_value\",\"type\":\"button\",\"action_ts\":\"1234567890.123456\"}]}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void messageAction() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"message_action\",\"token\":\"legacy-token\",\"action_ts\":\"1234567890.123456\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"user\":{\"id\":\"U123\",\"name\":\"User Name\"},\"channel\":{\"id\":\"C123\",\"name\":\"general\"},\"is_enterprise_install\":false,\"enterprise\":null,\"callback_id\":\"message_action_callback\",\"trigger_id\":\"trigger-id-123\",\"response_url\":\"https://hooks.slack.com/app/T123/123/abc\",\"message_ts\":\"1234567890.123456\",\"message\":{\"type\":\"message\",\"user\":\"U456\",\"text\":\"This is a message\",\"ts\":\"1234567890.123456\"}}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void viewSubmission() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"view_submission\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"trigger_id\":\"trigger-id-123\",\"view\":{\"id\":\"V123\",\"team_id\":\"T123\",\"type\":\"modal\",\"blocks\":[{\"type\":\"input\",\"block_id\":\"input_block\",\"label\":{\"type\":\"plain_text\",\"text\":\"Input Label\"},\"element\":{\"type\":\"plain_text_input\",\"action_id\":\"input_action\"}}],\"private_metadata\":\"\",\"callback_id\":\"view_callback\",\"state\":{\"values\":{\"input_block\":{\"input_action\":{\"type\":\"plain_text_input\",\"value\":\"User input value\"}}}},\"hash\":\"hash123\",\"title\":{\"type\":\"plain_text\",\"text\":\"Modal Title\"},\"clear_on_close\":false,\"notify_on_close\":false,\"close\":null,\"submit\":{\"type\":\"plain_text\",\"text\":\"Submit\"},\"previous_view_id\":null,\"root_view_id\":\"V123\",\"app_id\":\"A123\",\"external_id\":\"\",\"app_installed_team_id\":\"T123\",\"bot_id\":\"B123\"},\"response_urls\":[],\"is_enterprise_install\":false,\"enterprise\":null}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void viewClosed() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"view_closed\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"view\":{\"id\":\"V123\",\"team_id\":\"T123\",\"type\":\"modal\",\"callback_id\":\"view_callback\",\"hash\":\"hash123\",\"title\":{\"type\":\"plain_text\",\"text\":\"Modal Title\"}},\"is_enterprise_install\":false,\"enterprise\":null,\"is_cleared\":false}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void shortcut() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"shortcut\",\"token\":\"legacy-token\",\"action_ts\":\"1234567890.123456\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"user\":{\"id\":\"U123\",\"username\":\"user\",\"team_id\":\"T123\"},\"is_enterprise_install\":false,\"enterprise\":null,\"callback_id\":\"shortcut_callback\",\"trigger_id\":\"trigger-id-123\"}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void globalShortcut() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"shortcut\",\"token\":\"legacy-token\",\"action_ts\":\"1234567890.123456\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"user\":{\"id\":\"U123\",\"username\":\"user\",\"team_id\":\"T123\"},\"is_enterprise_install\":false,\"enterprise\":null,\"callback_id\":\"global_shortcut\",\"trigger_id\":\"trigger-id-123\"}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void workflowStepEdit() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"workflow_step_edit\",\"token\":\"legacy-token\",\"action_ts\":\"1234567890.123456\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"user\":{\"id\":\"U123\",\"username\":\"user\",\"team_id\":\"T123\"},\"callback_id\":\"workflow_step_callback\",\"trigger_id\":\"trigger-id-123\",\"workflow_step\":{\"workflow_step_edit_id\":\"WSE123\",\"workflow_id\":\"WF123\",\"step_id\":\"step123\"},\"is_enterprise_install\":false,\"enterprise\":null}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void selectMenuAction() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"block_actions\",\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"container\":{\"type\":\"message\",\"message_ts\":\"1234567890.123456\",\"channel_id\":\"C123\"},\"trigger_id\":\"trigger-id-123\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"channel\":{\"id\":\"C123\",\"name\":\"general\"},\"state\":{\"values\":{}},\"response_url\":\"https://hooks.slack.com/actions/T123/123/abc\",\"actions\":[{\"action_id\":\"select_action\",\"block_id\":\"block123\",\"selected_option\":{\"text\":{\"type\":\"plain_text\",\"text\":\"Option 1\"},\"value\":\"option_1\"},\"type\":\"static_select\",\"action_ts\":\"1234567890.123456\"}]}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void multiSelectMenuAction() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"block_actions\",\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"container\":{\"type\":\"message\",\"message_ts\":\"1234567890.123456\",\"channel_id\":\"C123\"},\"trigger_id\":\"trigger-id-123\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"channel\":{\"id\":\"C123\",\"name\":\"general\"},\"state\":{\"values\":{}},\"response_url\":\"https://hooks.slack.com/actions/T123/123/abc\",\"actions\":[{\"action_id\":\"multi_select_action\",\"block_id\":\"block123\",\"selected_options\":[{\"text\":{\"type\":\"plain_text\",\"text\":\"Option 1\"},\"value\":\"option_1\"},{\"text\":{\"type\":\"plain_text\",\"text\":\"Option 2\"},\"value\":\"option_2\"}],\"type\":\"multi_static_select\",\"action_ts\":\"1234567890.123456\"}]}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void datePickerAction() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"block_actions\",\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"container\":{\"type\":\"message\",\"message_ts\":\"1234567890.123456\",\"channel_id\":\"C123\"},\"trigger_id\":\"trigger-id-123\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"channel\":{\"id\":\"C123\",\"name\":\"general\"},\"state\":{\"values\":{}},\"response_url\":\"https://hooks.slack.com/actions/T123/123/abc\",\"actions\":[{\"action_id\":\"date_action\",\"block_id\":\"block123\",\"selected_date\":\"2023-12-25\",\"type\":\"datepicker\",\"action_ts\":\"1234567890.123456\"}]}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }

    @Test
    void overflowMenuAction() throws Exception {
        Trigger appTrigger = Trigger.builder()
            .id(IdUtils.create())
            .type(Trigger.class.getName())
            .key("zzzz")
            .signingSecret(Property.ofValue(signingSecret))
            .build();

        Flow flow = flow(appTrigger);

        String body = "{\"type\":\"block_actions\",\"user\":{\"id\":\"U123\",\"username\":\"user\",\"name\":\"User Name\",\"team_id\":\"T123\"},\"api_app_id\":\"A123\",\"token\":\"legacy-token\",\"container\":{\"type\":\"message\",\"message_ts\":\"1234567890.123456\",\"channel_id\":\"C123\"},\"trigger_id\":\"trigger-id-123\",\"team\":{\"id\":\"T123\",\"domain\":\"workspace\"},\"channel\":{\"id\":\"C123\",\"name\":\"general\"},\"state\":{\"values\":{}},\"response_url\":\"https://hooks.slack.com/actions/T123/123/abc\",\"actions\":[{\"action_id\":\"overflow_action\",\"block_id\":\"block123\",\"selected_option\":{\"text\":{\"type\":\"plain_text\",\"text\":\"Delete\"},\"value\":\"delete_action\"},\"type\":\"overflow\",\"action_ts\":\"1234567890.123456\"}]}";

        String requestBody = "payload=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        WebhookContext build = webhookContext(flow, appTrigger, requestBody);

        HttpResponse<?> evaluate = appTrigger.evaluate(build);

        assertResponse(evaluate);
    }
}
