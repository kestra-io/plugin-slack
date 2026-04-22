package io.kestra.plugin.slack.notifications;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.util.http.SlackHttpClient;
import com.slack.api.webhook.WebhookResponse;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackWebhookConnection;
import io.kestra.plugin.slack.MessagePayloadInterface;
import io.kestra.plugin.slack.services.MessageService;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;

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
            title = "Send a Slack message via incoming webhook with a text argument with 'messageText' (handles Slack markdown, no escaping needed)",
            full = true,
            code = """
                id: slack_incoming_webhook
                namespace: company.team

                tasks:
                  - id: send_slack_message
                    type: io.kestra.plugin.slack.notifications.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK') }}"
                    messageText: "Hello from the workflow {{ flow.id }}"
                """
        ),
        @Example(
            title = "Send a Slack notification on a failed flow execution with `payload`.",
            full = true,
            code = """
                id: unreliable_flow
                namespace: company.team

                tasks:
                  - id: fail
                    type: io.kestra.plugin.scripts.shell.Commands
                    taskRunner:
                      type: io.kestra.plugin.core.runner.Process
                    commands:
                      - exit 1

                errors:
                  - id: alert_on_failure
                    type: io.kestra.plugin.slack.notifications.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK') }}" # https://hooks.slack.com/services/xzy/xyz/xyz
                    messageText: "Failure alert for flow {{ flow.namespace }}.{{ flow.id }} with ID {{ execution.id }}"
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
                    type: io.kestra.plugin.slack.notifications.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK') }}"
                    payload: |
                      {
                        "blocks": [
                          {
                            "type": "header",
                            "text": {
                              "type": "plain_text",
                              "text": "Workflow completed: {{ flow.id }}"
                            }
                          },
                          {
                            "type": "divider"
                          },
                          {
                            "type": "section",
                            "fields": [
                              {
                                "type": "mrkdwn",
                                "text": "*Namespace:*\n{{ flow.namespace }}"
                              },
                              {
                                "type": "mrkdwn",
                                "text": "*Execution ID:*\n{{ execution.id }}"
                              },
                              {
                                "type": "mrkdwn",
                                "text": "*Status:*\n{{ execution.state }}"
                              },
                              {
                                "type": "mrkdwn",
                                "text": "*Start time:*\n{{ execution.startDate }}"
                              }
                            ]
                          },
                          {
                            "type": "context",
                            "elements": [
                              {
                                "type": "mrkdwn",
                                "text": "Triggered by Kestra"
                              }
                            ]
                          }
                        ]
                      }
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
                    type: io.kestra.plugin.slack.notifications.SlackIncomingWebhook
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
    aliases = {
        "io.kestra.plugin.notifications.slack.SlackIncomingWebhook",
        "io.kestra.plugin.slack.SlackIncomingWebhook",
    }
)
public class SlackIncomingWebhook extends AbstractSlackWebhookConnection implements MessagePayloadInterface {
    protected Property<String> payload;
    private Property<String> messageText;

    @Schema(
        title = "Slack incoming webhook URL",
        description = "Check the <a href=\"https://api.slack.com/messaging/webhooks#create_a_webhook\">Create an Incoming Webhook</a> documentation for more details."
    )
    @PluginProperty(dynamic = true, group = "connection", secret = true)
    @NotEmpty
    private String url;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        // Render variables once with 'r' prefix
        String rUrl = runContext.render(this.url);
        String rPayloadJson = MessageService.prepareMessageAsJson(runContext, this.payload, this.messageText);
        Logger logger = runContext.logger();

        logger.debug("Send Slack webhook: {}", rPayloadJson);

        // Check if custom headers are provided
        if (this.options != null && this.options.getHeaders() != null) {
            WebhookResponse response = sendWithCustomHeaders(runContext, rUrl, rPayloadJson);

            logger.debug(
                "Response: code={}, message={}, body={}",
                response.getCode(), response.getMessage(), response.getBody()
            );

            if (response.getCode() == 200) {
                logger.info("Request succeeded");
            } else {
                throw new IOException(
                    "Slack webhook request failed with status " + response.getCode() +
                        ": " + response.getMessage() + " - " + response.getBody()
                );
            }
        } else {
            try (Slack slack = createConfiguredSlackInstance(runContext)) {
                WebhookResponse response = slack.send(rUrl, rPayloadJson);

                logger.debug(
                    "Response: code={}, message={}, body={}",
                    response.getCode(), response.getMessage(), response.getBody()
                );

                if (response.getCode() == 200) {
                    logger.info("Request succeeded");
                } else {
                    throw new IOException(
                        "Slack webhook request failed with status " + response.getCode() +
                            ": " + response.getMessage() + " - " + response.getBody()
                    );
                }
            }
        }

        return null;
    }

    private Slack createConfiguredSlackInstance(RunContext runContext) throws Exception {
        SlackConfig config = new SlackConfig();

        if (options != null) {
            var rReadTimeout = runContext.render(options.getReadTimeout()).as(Duration.class).orElse(null);
            if (rReadTimeout != null) {
                config.setHttpClientReadTimeoutMillis((int) rReadTimeout.toMillis());
            }

            var rWriteTimeout = runContext.render(options.getReadIdleTimeout()).as(Duration.class).orElse(null);
            if (rWriteTimeout != null) {
                config.setHttpClientWriteTimeoutMillis((int) rWriteTimeout.toMillis());
            }

            var rCallTimeout = runContext.render(options.getConnectTimeout()).as(Duration.class).orElse(null);
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

        var rReadTimeout = runContext.render(options.getReadTimeout()).as(Duration.class).orElse(null);
        if (rReadTimeout != null) {
            config.setHttpClientReadTimeoutMillis((int) rReadTimeout.toMillis());
        }

        var rWriteTimeout = runContext.render(options.getReadIdleTimeout()).as(Duration.class).orElse(null);
        if (rWriteTimeout != null) {
            config.setHttpClientWriteTimeoutMillis((int) rWriteTimeout.toMillis());
        }

        var rCallTimeout = runContext.render(options.getConnectTimeout()).as(Duration.class).orElse(null);
        if (rCallTimeout != null) {
            config.setHttpClientCallTimeoutMillis((int) rCallTimeout.toMillis());
        }
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();

        if (rHeaders != null) {
            okHttpBuilder.addInterceptor(chain ->
            {
                Request.Builder requestBuilder = chain.request().newBuilder();
                rHeaders.forEach(requestBuilder::addHeader);
                return chain.proceed(requestBuilder.build());
            });
        }

        SlackHttpClient httpClient = new SlackHttpClient(okHttpBuilder.build());
        try (Slack slack = Slack.getInstance(config, httpClient)) {
            return slack.send(url, payloadJson);
        }
    }
}
