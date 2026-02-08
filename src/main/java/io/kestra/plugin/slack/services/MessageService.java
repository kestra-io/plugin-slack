package io.kestra.plugin.slack.services;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

import java.time.Instant;

public class MessageService {
    public static String prepareMessageAsJson(RunContext runContext, Property<String> payload, Property<String> messageText) throws Exception {
        if (payload != null) {
            String rPayload = runContext.render(payload).as(String.class).orElse(null);
            Object jsonNode = JacksonMapper.ofJson().readTree(rPayload);
            return JacksonMapper.ofJson().writeValueAsString(jsonNode);
        }

        if (messageText != null) {
            String rMessageText = runContext.render(messageText).as(String.class).orElseThrow();

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

    private static String toSlackMrkdwn(String text) {
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

    public static Instant fromSlackTimestamp(String slackTs) {
        if (slackTs == null) {
            return null;
        }

        if (slackTs.contains("T")) {
            return Instant.parse(slackTs);
        }

        String[] parts = slackTs.split("\\.");
        long seconds = Long.parseLong(parts[0]);
        // Slack uses microseconds (6 digits), Instant uses nanoseconds (9 digits)
        long nanos = Long.parseLong(parts[1]) * 1000;

        return Instant.ofEpochSecond(seconds, nanos);
    }

    public static Instant fromSlackTimestamp(Integer slackTs) {
        if (slackTs == null) {
            return null;
        }

        return Instant.ofEpochSecond(slackTs);
    }

    public static Instant fromSlackTimestamp(Long slackTs) {
        if (slackTs == null) {
            return null;
        }

        return Instant.ofEpochSecond(slackTs);
    }

    public static String toSlackTimestamp(Instant instant) {
        if (instant == null) {
            return null;
        }

        long seconds = instant.getEpochSecond();
        // Convert nanoseconds (9 digits) to microseconds (6 digits)
        int micros = instant.getNano() / 1000;

        if (micros == 0) {
            return String.valueOf(seconds);
        }

        // Use format to ensure the microsecond part is always 6 digits with leading zeros
        return String.format("%d.%06d", seconds, micros);
    }
}
