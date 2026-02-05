package io.kestra.plugin.slack;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
