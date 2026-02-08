package io.kestra.plugin.slack;

import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.Purpose;
import com.slack.api.model.ResponseMetadata;
import com.slack.api.model.Topic;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static io.kestra.plugin.slack.app.AbstractSlackClientTest.convertToSlack;

@Controller("/webhook-unit-test")
public class FakeWebhookController {
    public static String data;
    public static Map<String, String> headers = new HashMap<>();

    @Post
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    public HttpResponse<String> post(@Body String data) {
        FakeWebhookController.data = data;
        return HttpResponse.ok("ok");
    }

    @Post("/with-headers")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    public HttpResponse<String> postWithHeaders(HttpRequest<?> request, @Body String data) {

        FakeWebhookController.data = data;
        request.getHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });

        return HttpResponse.ok("ok");
    }

    @Post("/mock/conversationslist/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockConversationsList(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var response = new ConversationsListResponse();
        response.setOk(true);

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_abc") ? null : "cursor_abc");

        var list = IntStream.range(data.contains("cursor_abc") ? 10 : 0, data.contains("cursor_abc") ? 20 : 10)
            .mapToObj(i -> Conversation.builder()
                .id("test" + i)
                .build()
            )
            .toList();

        response.setResponseMetadata(metadata);
        response.setChannels(list);

        return HttpResponse.ok(convertToSlack(response));
    }

    @Post("/mock/conversationsmembers/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockConversationsMembers(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_xyz") ? null : "cursor_xyz");

        var members = IntStream.range(data.contains("cursor_xyz") ? 15 : 0, data.contains("cursor_xyz") ? 25 : 15)
            .mapToObj(i -> "U" + String.format("%010d", i))
            .toList();

        return HttpResponse.ok(convertToSlack(Map.of(
            "ok", true,
            "members", members,
            "response_metadata", metadata
        )));
    }

    @Post("/mock/conversationshistory/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockConversationsHistory(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_history") ? null : "cursor_history");

        var messages = IntStream.range(data.contains("cursor_history") ? 20 : 0, data.contains("cursor_history") ? 30 : 20)
            .mapToObj(i -> Map.of(
                "type", "message",
                "user", "U" + String.format("%010d", i),
                "text", "Message " + i,
                "ts", "1234567890." + String.format("%06d", i)
            ))
            .toList();

        return HttpResponse.ok(convertToSlack(Map.of(
            "ok", true,
            "messages", messages,
            "has_more", !data.contains("cursor_history"),
            "response_metadata", metadata
        )));
    }

    @Get("/mock/conversations/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockConversation(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        return HttpResponse.ok(convertToSlack(Conversation.builder()
            .id("test")
            .build()
        ));
    }

    @Post("/mock/conversations/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockConversationPost(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        Purpose purpose = new Purpose();
        purpose.setValue("purpose");

        Topic topic = new Topic();
        topic.setValue("purpose");

        return HttpResponse.ok(convertToSlack(Map.of("ok", "true", "channel", Conversation.builder()
            .id("test")
            .purpose(purpose)
            .topic(topic)
            .build()
        )));
    }

    @Get("/mock/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockGet(HttpRequest<?> request, String method, @Body String data) {
        FakeWebhookController.data = data;

        return HttpResponse.ok(Map.of("ok", "true"));
    }

    @Post("/mock/{method}")
    @Consumes({MediaType.ALL})
    public HttpResponse<?> mockPost(HttpRequest<?> request, String method, @Body String data) {
        FakeWebhookController.data = data;

        return HttpResponse.ok(Map.of("ok", "true"));
    }
}
