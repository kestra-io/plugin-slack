package io.kestra.plugin.slack;

import java.io.IOException;
import java.net.Proxy;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiTextResponse;
import com.slack.api.util.http.SlackHttpClient;

import io.kestra.core.http.client.configurations.ProxyConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.Rethrow;
import io.kestra.plugin.slack.services.SlackProxyService;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSlackClientConnection extends Task {
    @Schema(
        title = "Slack token"
    )
    @NotNull
    @PluginProperty(group = "main", secret = true)
    protected Property<String> token;

    @Schema(
        title = "Proxy configuration",
        description = """
            Optional proxy used for every request made by this task. The proxy is active only when `address` is set, and `type` must then be set to `HTTP` or `SOCKS` (the default `DIRECT` means no proxy and is rejected when an address is given). \
            Set `username` and `password` for Basic proxy authentication, using secrets such as `{{ secret('PROXY_PASSWORD') }}`; SOCKS proxies do not support credentials here. \
            An explicit proxy overrides the JVM (`http.proxyHost`...) and environment (`HTTP_PROXY`, `HTTPS_PROXY`) proxy settings."""
    )
    @PluginProperty(group = "connection")
    protected ProxyConfiguration proxy;

    // we only set in tests to redirect API calls to a mock server.
    protected transient String methodsEndpointUrlPrefix;

    protected <R extends SlackApiTextResponse> R call(RunContext runContext, Rethrow.FunctionChecked<MethodsClient, R, Exception> call) {
        SlackConfig config = new SlackConfig();
        if (methodsEndpointUrlPrefix != null) {
            config.setMethodsEndpointUrlPrefix(methodsEndpointUrlPrefix);
            config.setStatsEnabled(false);
        }
        OkHttpClient.Builder proxied = proxiedClientBuilder(runContext, config);

        try (Slack slack = proxied == null ? Slack.getInstance(config) : Slack.getInstance(config, new SlackHttpClient(proxied.build()))) {
            R response = call.apply(slack.methods(runContext.render(this.token).as(String.class).orElseThrow()));

            if (response.getWarning() != null) {
                runContext.logger().warn(response.getWarning());
            }

            if (response.isOk()) {
                return response;
            }

            throw new IllegalStateException(response.getError() + ": " + response);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Validation errors are thrown unwrapped so they stay actionable.
    private OkHttpClient.Builder proxiedClientBuilder(RunContext runContext, SlackConfig config) {
        try {
            if (this.proxy == null) {
                return null;
            }
            var rAddress = runContext.render(proxy.getAddress()).as(String.class).filter(s -> !s.isBlank()).orElse(null);
            if (rAddress == null) {
                return null;
            }

            var rType = runContext.render(proxy.getType()).as(Proxy.Type.class).orElse(Proxy.Type.DIRECT);
            if (rType == Proxy.Type.DIRECT) {
                throw new IllegalArgumentException("`proxy.address` is set but `proxy.type` is `DIRECT`, which means no proxy. Set `proxy.type: HTTP` (or `SOCKS`) to use the proxy.");
            }
            var rPort = runContext.render(proxy.getPort()).as(Integer.class)
                .orElseThrow(() -> new IllegalArgumentException("`proxy.port` is required when `proxy.address` is set."));
            if (rPort < 1 || rPort > 65535) {
                throw new IllegalArgumentException("`proxy.port` must be between 1 and 65535.");
            }
            var rUsername = runContext.render(proxy.getUsername()).as(String.class).filter(s -> !s.isEmpty()).orElse(null);
            var rPassword = runContext.render(proxy.getPassword()).as(String.class).filter(s -> !s.isEmpty()).orElse(null);
            if ((rUsername == null) != (rPassword == null)) {
                throw new IllegalArgumentException("`proxy.username` and `proxy.password` must be set together: set both for Basic proxy authentication, or neither.");
            }
            if (rType == Proxy.Type.SOCKS && rUsername != null) {
                throw new IllegalArgumentException("Proxy credentials are not supported with a SOCKS proxy. Remove `proxy.username` and `proxy.password`, or use `proxy.type: HTTP`.");
            }

            // drop the SDK's own system/env proxy (and its credentials): the explicit proxy wins
            config.setProxyUrl(null);
            config.setProxyHeaders(null);

            var builder = SlackHttpClient.buildOkHttpClient(config).newBuilder();
            SlackProxyService.apply(builder, rType, rAddress, rPort, rUsername, rPassword);

            var message = "Proxy authentication failed (HTTP 407) for %s:%d: check the `proxy.username` and `proxy.password` values.".formatted(rAddress, rPort);
            builder.addInterceptor(chain ->
            {
                Response response;
                try {
                    response = chain.proceed(chain.request());
                } catch (IOException e) {
                    // OkHttp throws a plain IOException when the CONNECT tunnel to an HTTPS target is refused with 407
                    if ("Failed to authenticate with proxy".equals(e.getMessage())) {
                        throw new IOException(message, e);
                    }
                    throw e;
                }
                if (response.code() == 407) {
                    response.close();
                    throw new IOException(message);
                }
                return response;
            });
            return builder;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
