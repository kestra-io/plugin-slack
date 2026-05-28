package io.kestra.plugin.slack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.model.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import static io.kestra.plugin.slack.app.AbstractSlackClientTest.convertToSlack;

@Controller("/webhook-unit-test")
public class FakeWebhookController {
    public static String data;
    public static Map<String, String> headers = new HashMap<>();

    @Post
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<String> post(@Body String data) {
        FakeWebhookController.data = data;
        return HttpResponse.ok("ok");
    }

    @Post("/with-headers")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<String> postWithHeaders(HttpRequest<?> request, @Body String data) {

        FakeWebhookController.data = data;
        request.getHeaders().forEach((name, values) ->
        {
            if (!values.isEmpty()) {
                headers.put(name, values.getFirst());
            }
        });

        return HttpResponse.ok("ok");
    }

    @Post("/status-201")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<String> postReturning201(@Body String data) {
        FakeWebhookController.data = data;
        return HttpResponse.created("ok");
    }

    @Post("/status-204")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<Void> postReturning204(@Body String data) {
        FakeWebhookController.data = data;
        return HttpResponse.noContent();
    }

    @Post("/status-400")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<String> postReturning400(@Body String data) {
        FakeWebhookController.data = data;
        return HttpResponse.badRequest("invalid_payload");
    }

    @Post("/mock/conversationslist/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockConversationsList(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var response = new ConversationsListResponse();
        response.setOk(true);

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_abc") ? null : "cursor_abc");

        var list = IntStream.range(data.contains("cursor_abc") ? 10 : 0, data.contains("cursor_abc") ? 20 : 10)
            .mapToObj(
                i -> Conversation.builder()
                    .id("test" + i)
                    .build()
            )
            .toList();

        response.setResponseMetadata(metadata);
        response.setChannels(list);

        return HttpResponse.ok(convertToSlack(response));
    }

    @Post("/mock/conversationsmembers/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockConversationsMembers(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_xyz") ? null : "cursor_xyz");

        var members = IntStream.range(data.contains("cursor_xyz") ? 15 : 0, data.contains("cursor_xyz") ? 25 : 15)
            .mapToObj(i -> "U" + String.format("%010d", i))
            .toList();

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "members", members,
                    "response_metadata", metadata
                )
            )
        );
    }

    @Post("/mock/conversationshistory/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockConversationsHistory(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_history") ? null : "cursor_history");

        var messages = IntStream.range(data.contains("cursor_history") ? 20 : 0, data.contains("cursor_history") ? 30 : 20)
            .mapToObj(
                i -> Map.of(
                    "type", "message",
                    "user", "U" + String.format("%010d", i),
                    "text", "Message " + i,
                    "ts", "1234567890." + String.format("%06d", i)
                )
            )
            .toList();

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "messages", messages,
                    "has_more", !data.contains("cursor_history"),
                    "response_metadata", metadata
                )
            )
        );
    }

    @Post("/mock/reactionsget/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockReactionsGet(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var reactions = IntStream.range(0, 20)
            .mapToObj(
                i -> Reaction.builder()
                    .name("thumbsup-" + i)
                    .users(List.of("U1234567890", "U0987654321"))
                    .count(2)
                    .build()
            )
            .toList();

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "message", Map.of(
                        "reactions", reactions
                    )
                )
            )
        );
    }

    @Get("/mock/conversations/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockConversation(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        return HttpResponse.ok(
            convertToSlack(
                Conversation.builder()
                    .id("test")
                    .build()
            )
        );
    }

    @Post("/mock/conversations/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockConversationPost(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        Purpose purpose = new Purpose();
        purpose.setValue("purpose");

        Topic topic = new Topic();
        topic.setValue("purpose");

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", "true", "channel", Conversation.builder()
                        .id("test")
                        .purpose(purpose)
                        .topic(topic)
                        .build()
                )
            )
        );
    }

    @Get("/mock/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockGet(HttpRequest<?> request, String method, @Body String data) {
        FakeWebhookController.data = data;

        return HttpResponse.ok(Map.of("ok", "true"));
    }

    @Post("/mock/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockPost(HttpRequest<?> request, String method, @Body String data) {
        FakeWebhookController.data = data;

        // Mock canvas method responses
        if (method.contains("canvases")) {
            if (method.contains("create") && !method.contains("conversations")) {
                return HttpResponse.ok(Map.of("ok", true, "canvas_id", "F1234567890"));
            } else if (method.contains("sectionslookup") || method.contains("sections.lookup")) {
                return HttpResponse.ok(Map.of("ok", true, "sections", List.of()));
            }
        }

        return HttpResponse.ok(Map.of("ok", "true"));
    }

    @Post("/mock/users/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockUsers(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var profile = Map.ofEntries(
            Map.entry("title", "Software Engineer"),
            Map.entry("phone", "+1234567890"),
            Map.entry("real_name", "John Doe"),
            Map.entry("display_name", "johndoe"),
            Map.entry("email", "john.doe@example.com"),
            Map.entry("image_24", "https://example.com/avatar_24.jpg"),
            Map.entry("image_32", "https://example.com/avatar_32.jpg"),
            Map.entry("image_48", "https://example.com/avatar_48.jpg"),
            Map.entry("image_72", "https://example.com/avatar_72.jpg"),
            Map.entry("image_192", "https://example.com/avatar_192.jpg"),
            Map.entry("image_512", "https://example.com/avatar_512.jpg")
        );

        var user = Map.ofEntries(
            Map.entry("id", "U1234567890"),
            Map.entry("team_id", "T1234567890"),
            Map.entry("name", "johndoe"),
            Map.entry("deleted", false),
            Map.entry("real_name", "John Doe"),
            Map.entry("tz", "America/New_York"),
            Map.entry("tz_label", "Eastern Daylight Time"),
            Map.entry("tz_offset", -14400),
            Map.entry("profile", profile),
            Map.entry("is_admin", false),
            Map.entry("is_owner", false),
            Map.entry("is_bot", false),
            Map.entry("updated", 1234567890)
        );

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "user", user
                )
            )
        );
    }

    @Post("/mock/userslist/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockUsersList(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_users") ? null : "cursor_users");

        var members = IntStream.range(data.contains("cursor_users") ? 10 : 0, data.contains("cursor_users") ? 15 : 10)
            .mapToObj(i ->
            {
                var profile = Map.of(
                    "real_name", "User " + i,
                    "display_name", "user" + i,
                    "email", "user" + i + "@example.com"
                );

                return Map.of(
                    "id", "U" + String.format("%010d", i),
                    "team_id", "T1234567890",
                    "name", "user" + i,
                    "deleted", false,
                    "profile", profile,
                    "is_bot", false
                );
            })
            .toList();

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "members", members,
                    "response_metadata", metadata
                )
            )
        );
    }

    @Post("/mock/usersconversations/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockUsersConversations(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var metadata = new ResponseMetadata();
        metadata.setNextCursor(data.contains("cursor_conv") ? null : "cursor_conv");

        var channels = IntStream.range(data.contains("cursor_conv") ? 5 : 0, data.contains("cursor_conv") ? 10 : 5)
            .mapToObj(
                i -> Conversation.builder()
                    .id("C" + String.format("%010d", i))
                    .name("channel-" + i)
                    .build()
            )
            .toList();

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "channels", channels,
                    "response_metadata", metadata
                )
            )
        );
    }

    @Post("/mock/usersgetpresence/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockGetPresence(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "presence", "active",
                    "online", true,
                    "auto_away", false,
                    "manual_away", false,
                    "connection_count", 2,
                    "last_activity", 1234567890
                )
            )
        );
    }

    @Post("/mock/usersprofile/{method}")
    @Consumes({ MediaType.ALL })
    public HttpResponse<?> mockUsersProfile(HttpRequest<?> request, @Body String data) {
        FakeWebhookController.data = data;

        var profile = Map.ofEntries(
            Map.entry("title", "Software Engineer"),
            Map.entry("phone", "+1234567890"),
            Map.entry("real_name", "John Doe"),
            Map.entry("display_name", "johndoe"),
            Map.entry("email", "john.doe@example.com"),
            Map.entry("status_text", "In a meeting"),
            Map.entry("status_emoji", ":calendar:")
        );

        return HttpResponse.ok(
            convertToSlack(
                Map.of(
                    "ok", true,
                    "profile", profile
                )
            )
        );
    }
}
