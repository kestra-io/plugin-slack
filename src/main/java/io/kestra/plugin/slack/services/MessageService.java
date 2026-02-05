package io.kestra.plugin.slack.services;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

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
}
