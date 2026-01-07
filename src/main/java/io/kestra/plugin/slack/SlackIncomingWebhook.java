package io.kestra.plugin.slack;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.webhook.WebhookResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.util.Map;

import okhttp3.*;
import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Send a Slack message using an Incoming Webhook.",
    description = "Add this task to send direct Slack notifications. Check the <a href=\"https://api.slack.com/messaging/webhooks\">Slack documentation</a> for more details."
)
@Plugin(
    examples = {
        @Example(
            title = "Send a Slack notification on a failed flow execution.",
            full = true,
            code = """
                id: unreliable_flow
                namespace: company.team

                tasks:
                  - id: fail
                    type: io.kestra.plugin.scripts.shell.Commands
                    runner: PROCESS
                    commands:
                      - exit 1

                errors:
                  - id: alert_on_failure
                    type: io.kestra.plugin.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK') }}" # https://hooks.slack.com/services/xzy/xyz/xyz
                    payload: |
                      {
                        "text": "Failure alert for flow {{ flow.namespace }}.{{ flow.id }} with ID {{ execution.id }}"
                      }
                """
        ),
        @Example(
            title = "Send a Slack message via incoming webhook with a text argument.",
            full = true,
            code = """
                id: slack_incoming_webhook
                namespace: company.team

                tasks:
                  - id: send_slack_message
                    type: io.kestra.plugin.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK') }}"
                    payload: |
                      {
                        "text": "Hello from the workflow {{ flow.id }}"
                      }
                """
        ),
        @Example(
            title = "Send a Slack message via incoming webhook with a blocks argument, read more on blocks <a href=\"https://api.slack.com/reference/block-kit/blocks\">here</a>.",
            full = true,
            code = """
                id: slack_incoming_webhook
                namespace: company.team

                tasks:
                  - id: send_slack_message
                    type: io.kestra.plugin.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK') }}"
                    payload: |
                      {
                        "blocks": [
                    		{
                    			"type": "section",
                    			"text": {
                    				"type": "mrkdwn",
                    				"text": "Hello from the workflow *{{ flow.id }}*"
                    			}
                    		}
                    	]
                      }
                """
        ),
        @Example(
            title = "Send a Slack message with 'messageText' (handles Slack markdown, no escaping needed)",
            full = true,
            code = """
                id: slack_incoming_webhook
                namespace: company.team

                inputs:
                 - id: prompt
                   type: STRING
                   defaults: Summarize top 5 news from my region.

                tasks:
                 - id: news
                   type: io.kestra.plugin.openai.Responses
                   apiKey: "{{ kv('OPENAI_API_KEY') }}"
                   model: gpt-4.1-mini
                   input: "{{ inputs.prompt }}"
                   toolChoice: REQUIRED
                   tools:
                     - type: web_search_preview
                       search_context_size: low
                       user_location:
                         type: approximate
                         city: Berlin
                         region: Berlin
                         country: DE

                 - id: send_via_slack
                   type: io.kestra.plugin.slack.SlackIncomingWebhook
                   url: https://kestra.io/api/mock
                   messageText: "Current news from Berlin: {{ outputs.news.outputText }}"
                """
        ),
        @Example(
            title = "Send a Rocket Chat message via [Slack incoming webhook](https://docs.rocket.chat/docs/integrations#incoming-webhook-script).",
            full = true,
            code = """
                id: rocket_chat_notification
                namespace: company.team
                tasks:
                  - id: send_rocket_chat_message
                    type: io.kestra.plugin.slack.SlackIncomingWebhook
                    url: "{{ secret('ROCKET_CHAT_WEBHOOK') }}"
                    payload: |
                      {
                        "alias": "Kestra TEST",
                        "avatar": "https://avatars.githubusercontent.com/u/59033362?s=48",
                        "emoji": ":smirk:",
                        "roomId": "#my-channel",
                        "text": "Sample",
                        "tmshow": true,
                        "attachments": [
                          {
                            "collapsed": false,
                            "color": "#ff0000",
                            "text": "Yay!",
                            "title": "Attachment Example",
                            "title_link": "https://rocket.chat",
                            "title_link_download": false,
                            "fields": [
                              {
                                "short": false,
                                "title": "Test title",
                                "value": "Test value"
                              },
                              {
                                "short": true,
                                "title": "Test title",
                                "value": "Test value"
                              }
                            ]
                          }
                        ]
                      }
                """
        ),
    },
    aliases = "io.kestra.plugin.notifications.slack.SlackIncomingWebhook"
)
public class SlackIncomingWebhook extends AbstractSlackConnection {
    @Schema(
        title = "Slack incoming webhook URL",
        description = "Check the <a href=\"https://api.slack.com/messaging/webhooks#create_a_webhook\">Create an Incoming Webhook</a> documentation for more details."
    )
    @PluginProperty(dynamic = true)
    @NotEmpty
    private String url;

    @Schema(
        title = "Slack message payload"
    )
    protected Property<String> payload;

    @Schema(
        title = "Message Text or JSON String",
        description = "The message content as a raw string. It can be plain text with markdown, or a JSON object. If not a valid JSON object, it is automatically wrapped in `{\"text\": \"...\"}`. This property is ignored if the `payload` property is set."
    )
    private Property<String> messageText;

  @Override
  public VoidOutput run(RunContext runContext) throws Exception {
    // Render variables once with 'r' prefix
    String rUrl = runContext.render(this.url);
    String rPayloadJson = prepareMessageAsJson(runContext);
    Logger logger = runContext.logger();

    logger.debug("Send Slack webhook: {}", rPayloadJson);

    // Check if custom headers are provided
    if (this.options != null && this.options.getHeaders() != null) {
      WebhookResponse response = sendWithCustomHeaders(runContext, rUrl, rPayloadJson);

      logger.debug("Response: code={}, message={}, body={}",
          response.getCode(), response.getMessage(), response.getBody());

      if (response.getCode() == 200) {
        logger.info("Request succeeded");
      } else {
        throw new IOException("Slack webhook request failed with status " + response.getCode() +
            ": " + response.getMessage() + " - " + response.getBody());
      }
    } else {
      Slack slack = createConfiguredSlackInstance(runContext);
      WebhookResponse response = slack.send(rUrl, rPayloadJson);

      logger.debug("Response: code={}, message={}, body={}",
          response.getCode(), response.getMessage(), response.getBody());

      if (response.getCode() == 200) {
        logger.info("Request succeeded");
      } else {
        throw new IOException("Slack webhook request failed with status " + response.getCode() +
            ": " + response.getMessage() + " - " + response.getBody());
      }
    }

    return null;
  }

  private Slack createConfiguredSlackInstance(RunContext runContext) throws Exception {
    SlackConfig config = new SlackConfig();

    if (options != null) {
      var rReadTimeout = runContext.render(options.getReadTimeout()).as(java.time.Duration.class).orElse(null);
      if (rReadTimeout != null) {
        config.setHttpClientReadTimeoutMillis((int) rReadTimeout.toMillis());
      }

      var rWriteTimeout = runContext.render(options.getReadIdleTimeout()).as(java.time.Duration.class).orElse(null);
      if (rWriteTimeout != null) {
        config.setHttpClientWriteTimeoutMillis((int) rWriteTimeout.toMillis());
      }

      var rCallTimeout = runContext.render(options.getConnectTimeout()).as(java.time.Duration.class).orElse(null);
      if (rCallTimeout != null) {
        config.setHttpClientCallTimeoutMillis((int) rCallTimeout.toMillis());
      }
    }

    return Slack.getInstance(config);
  }

  private WebhookResponse sendWithCustomHeaders(RunContext runContext, String url, String payloadJson)
      throws Exception {
    Map<String, String> rHeaders = runContext.render(this.options.getHeaders())
        .asMap(String.class, String.class);

    SlackConfig config = new SlackConfig();

    var rReadTimeout = runContext.render(options.getReadTimeout()).as(java.time.Duration.class).orElse(null);
    if (rReadTimeout != null) {
      config.setHttpClientReadTimeoutMillis((int) rReadTimeout.toMillis());
    }

    var rWriteTimeout = runContext.render(options.getReadIdleTimeout()).as(java.time.Duration.class).orElse(null);
    if (rWriteTimeout != null) {
      config.setHttpClientWriteTimeoutMillis((int) rWriteTimeout.toMillis());
    }

    var rCallTimeout = runContext.render(options.getConnectTimeout()).as(java.time.Duration.class).orElse(null);
    if (rCallTimeout != null) {
      config.setHttpClientCallTimeoutMillis((int) rCallTimeout.toMillis());
    }

    Slack slack = Slack.getInstance(config);

    OkHttpClient httpClient = slack.getHttpClient().getOkHttpClient();

    Request.Builder requestBuilder = new Request.Builder()
        .url(url)
        .post(RequestBody.create(payloadJson, MediaType.parse("application/json; charset=utf-8")));

    if (rHeaders != null) {
      rHeaders.forEach(requestBuilder::addHeader);
    }

    Request request = requestBuilder.build();

    try (Response response = httpClient.newCall(request).execute()) {
      String body = response.body() != null ? response.body().string() : "";

      return WebhookResponse.builder()
          .code(response.code())
          .message(response.message())
          .body(body)
          .build();
    }
  }

  private String prepareMessageAsJson(RunContext runContext) throws Exception {
    if (payload != null) {
      String rPayload = runContext.render(payload).as(String.class).orElse(null);
      Object jsonNode = JacksonMapper.ofJson().readTree(rPayload);
      return JacksonMapper.ofJson().writeValueAsString(jsonNode);
    }

    if (messageText != null) {
      String rMessageText = runContext.render(this.messageText).as(String.class).orElseThrow();

      try {
        // first we try as Json for more flexibility
        Object jsonNode = JacksonMapper.ofJson().readTree(rMessageText);
        return JacksonMapper.ofJson().writeValueAsString(jsonNode);
      } catch (Exception e) {
        // not valid Json, so proceed with markdown text
        String rMessageTextMrkdwn = toSlackMrkdwn(rMessageText);
        return JacksonMapper.ofJson().writeValueAsString(
            JacksonMapper.ofJson().createObjectNode().put("text", rMessageTextMrkdwn));
      }
    }

    throw new IllegalArgumentException("Either 'messageText' or 'payload' must be provided");
  }

  private String toSlackMrkdwn(String text) {
    if (text == null)
      return null;
    // for bold text
    text = text.replaceAll("\\*\\*(.*?)\\*\\*", "*$1*");
    // for italic text
    text = text.replaceAll("__(.*?)__", "_$1_");
    // for links
    text = text.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<$2|$1>");
    return text;
  }
}
