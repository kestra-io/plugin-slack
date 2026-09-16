<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png"  alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

<p align="center">
  <a href="https://twitter.com/kestra_io" style="margin: 0 10px;">
        <img src="https://kestra.io/twitter.svg" alt="twitter" width="35" height="25" /></a>
  <a href="https://www.linkedin.com/company/kestra/" style="margin: 0 10px;">
        <img src="https://kestra.io/linkedin.svg" alt="linkedin" width="35" height="25" /></a>
  <a href="https://www.youtube.com/@kestra-io" style="margin: 0 10px;">
        <img src="https://kestra.io/youtube.svg" alt="youtube" width="35" height="25" /></a>
</p>

<br />
<p align="center">
    <a href="https://go.kestra.io/video/product-overview" target="_blank">
        <img src="https://kestra.io/startvideo.png" alt="Get started in 3 minutes with Kestra" width="640px" />
    </a>
</p>
<p align="center" style="color:grey;"><i>Get started with Kestra in 3 minutes.</i></p>

# Kestra Slack Plugin

## Why

- What user problem does this solve? Teams need to slack notification webhooks and full Slack App automations across chat, files, canvases, channels, reactions, users, and events from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Slack steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Slack.

## What

- Provides plugin components under `io.kestra.plugin.slack`.
- Includes classes such as `SlackTemplate`, `SlackIncomingWebhook`, `SlackExecution`, `MessageService`.

## Examples

### Include error logs in a Slack failure alert

The `errorLogs()` Pebble function only resolves logs for the current execution, so it is not available inside a `Flow` trigger that reacts to *other* flows failing. To surface those logs in a Slack alert, fetch them explicitly with `io.kestra.plugin.core.log.Fetch`, convert them to JSON, then read them back in the Slack payload:

```yaml
id: dbt_failure_notification
namespace: alerts

triggers:
  - id: dbt_job_failure
    type: io.kestra.plugin.core.trigger.Flow
    states:
      - FAILED

tasks:
  - id: get_error_logs
    type: io.kestra.plugin.core.log.Fetch
    level: ERROR
    executionId: "{{ trigger.executionId ?? execution.id }}"
    namespace: "{{ trigger.namespace ?? 'manual' }}"
    flowId: "{{ trigger.flowId ?? 'triggered' }}"

  - id: read_error_logs
    type: io.kestra.plugin.serdes.json.IonToJson
    from: "{{ outputs.get_error_logs.uri }}"
    newLine: false

  - id: slack_failure_alert
    type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
    url: "{{ secret('SLACK_WEBHOOK') }}"
    payload: |
      {% set errorLogs = fromJson(read(outputs.read_error_logs.uri)) %}
      {
        "blocks": [
          {
            "type": "header",
            "text": {
              "type": "plain_text",
              "text": ":warning: Flow \"{{ errorLogs[0].flowId }}\" failed",
              "emoji": true
            }
          },
          {% for messages in errorLogs %}
          {
            "type": "section",
            "text": {
              "type": "mrkdwn",
              "text": "```{{ messages.message }}```"
            }
          },
          {% endfor %}
        ]
      }
```

See [plugin-slack#87](https://github.com/kestra-io/plugin-slack/issues/87) for the discussion behind this pattern.

## Documentation
* Full documentation can be found under: [kestra.io/docs](https://kestra.io/docs)
* Documentation for developing a plugin is included in the [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)


## License
Apache 2.0 © [Kestra Technologies](https://kestra.io)


## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)
