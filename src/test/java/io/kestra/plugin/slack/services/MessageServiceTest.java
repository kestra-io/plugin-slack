package io.kestra.plugin.slack.services;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@KestraTest
class MessageServiceTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void shouldWrapNonObjectJsonAsText() {
        assertAll(Stream.of("123", "true", "null", "[1, 2]", "\"quoted\"").map(messageText -> () ->
        {
            var json =MessageService.prepareMessageAsJson(runContextFactory.of(), null, Property.ofValue(messageText));

            assertThat(json).as("messageText: %s", messageText).isEqualTo(
                JacksonMapper.ofJson().writeValueAsString(
                    JacksonMapper.ofJson().createObjectNode().put("text", messageText)
                )
            );
        }));
    }

    @Test
    void shouldWrapTextThatOnlyStartsWithJson() {
        assertAll(Stream.of(
            "3 alerts:\n• current ratio 0.77 is below the minimum of 1.00",
            "true story",
            "null and void",
            "\"quoted\" text",
            "{\"text\": \"hi\"} and some notes",
            "{\"text\": \"hi\"}{\"text\": \"again\"}",
            "{\"text\": \"unterminated\"",
            "{'text': 'single quotes'}",
            "",
            "   "
        ).map(messageText -> () ->
        {
            var json =MessageService.prepareMessageAsJson(runContextFactory.of(), null, Property.ofValue(messageText));

            assertThat(json).as("messageText: %s", messageText).isEqualTo(
                JacksonMapper.ofJson().writeValueAsString(
                    JacksonMapper.ofJson().createObjectNode().put("text", messageText)
                )
            );
        }));
    }

    @Test
    void shouldPreserveJsonObjectSurroundedByWhitespace() throws Exception {
        var json =MessageService.prepareMessageAsJson(runContextFactory.of(), null, Property.ofValue("  {\"text\": \"hi\"}\n"));

        assertThat(json).isEqualTo("{\"text\":\"hi\"}");
    }

    @Test
    void shouldPreserveJsonObject() throws Exception {
        var messageText ="{\"text\":\"**bold**\",\"blocks\":[]}";

        var json =MessageService.prepareMessageAsJson(runContextFactory.of(), null, Property.ofValue(messageText));

        assertThat(json).isEqualTo(messageText);
    }

    @Test
    void shouldWrapMarkdownAsSlackMrkdwn() throws Exception {
        var messageText ="**bold** __italic__ [link](https://example.com)";

        var json =MessageService.prepareMessageAsJson(runContextFactory.of(), null, Property.ofValue(messageText));

        assertThat(json).isEqualTo("{\"text\":\"*bold* _italic_ <https://example.com|link>\"}");
    }

    @Test
    void shouldPreferPayloadOverMessageText() throws Exception {
        var payload ="{\"text\":\"**bold**\",\"blocks\":[]}";

        var json =MessageService.prepareMessageAsJson(runContextFactory.of(), Property.ofValue(payload), Property.ofValue("123"));

        assertThat(json).isEqualTo(payload);
    }
}
