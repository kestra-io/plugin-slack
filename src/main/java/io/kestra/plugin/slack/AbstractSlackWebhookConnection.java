package io.kestra.plugin.slack;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSlackWebhookConnection extends Task implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Options",
        description = "The options to set to customize the HTTP client"
    )
    @PluginProperty(dynamic = true, group = "advanced")
    protected RequestOptions options;

    @Getter
    @Builder
    public static class RequestOptions {
        @Schema(title = "The time allowed to establish a connection to the server before failing")
        @PluginProperty(group = "execution")
        private final Property<Duration> connectTimeout;

        @Schema(title = "The maximum time allowed for reading data from the server before failing")
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> readTimeout = Property.ofValue(Duration.ofSeconds(10));

        @Schema(title = "The time allowed for a read connection to remain idle before closing it")
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> readIdleTimeout = Property.ofValue(Duration.of(5, ChronoUnit.MINUTES));

        @Schema(title = "The time an idle connection can remain in the client's connection pool before being closed")
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> connectionPoolIdleTimeout = Property.ofValue(Duration.ofSeconds(0));

        @Schema(title = "The maximum content length of the response")
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Integer> maxContentLength = Property.ofValue(1024 * 1024 * 10);

        @Schema(title = "The default charset for the request")
        @Builder.Default
        @PluginProperty(group = "advanced")
        private final Property<Charset> defaultCharset = Property.ofValue(StandardCharsets.UTF_8);

        @Schema(
            title = "HTTP headers",
            description = "HTTP headers to include in the request"
        )
        @PluginProperty(group = "advanced")
        public Property<Map<String, String>> headers;

        @Schema(
            title = "Proxy configuration",
            description = "The proxy configuration used to route outbound HTTP requests. " +
                "Useful when Kestra runs in a network that requires an outbound proxy to reach the internet."
        )
        @PluginProperty(group = "advanced")
        private ProxyOptions proxy;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProxyOptions {
        @Schema(
            title = "Proxy type",
            description = "The type of proxy: HTTP or SOCKS."
        )
        @Builder.Default
        @PluginProperty
        private Property<ProxyType> type = Property.ofValue(ProxyType.HTTP);

        @Schema(
            title = "Proxy host",
            description = "The hostname or IP address of the proxy server."
        )
        @PluginProperty
        private Property<String> address;

        @Schema(
            title = "Proxy port",
            description = "The port of the proxy server."
        )
        @PluginProperty
        private Property<Integer> port;

        @Schema(
            title = "Proxy username",
            description = "The username for proxy authentication (optional)."
        )
        @PluginProperty(secret = true)
        private Property<String> username;

        @Schema(
            title = "Proxy password",
            description = "The password for proxy authentication (optional)."
        )
        @PluginProperty(secret = true)
        private Property<String> password;
    }

    public enum ProxyType {
        HTTP,
        SOCKS;

        public Proxy.Type toJavaProxyType() {
            return switch (this) {
                case HTTP -> Proxy.Type.HTTP;
                case SOCKS -> Proxy.Type.SOCKS;
            };
        }
    }

    /**
     * Applies any configured proxy settings to the given OkHttpClient.Builder.
     *
     * @param builder    the OkHttpClient builder to configure
     * @param runContext the run context used to render Property values
     * @throws Exception if property rendering fails
     */
    protected void applyProxy(OkHttpClient.Builder builder, RunContext runContext) throws Exception {
        if (this.options == null || this.options.getProxy() == null) {
            return;
        }

        ProxyOptions proxyOptions = this.options.getProxy();

        String rAddress = runContext.render(proxyOptions.getAddress()).as(String.class).orElse(null);
        Integer rPort = runContext.render(proxyOptions.getPort()).as(Integer.class).orElse(null);

        if (rAddress == null || rPort == null) {
            return;
        }

        ProxyType rProxyType = runContext.render(proxyOptions.getType())
            .as(ProxyType.class)
            .orElse(ProxyType.HTTP);

        Proxy proxy = new Proxy(rProxyType.toJavaProxyType(), new InetSocketAddress(rAddress, rPort));
        builder.proxy(proxy);

        String rUsername = runContext.render(proxyOptions.getUsername()).as(String.class).orElse(null);
        String rPassword = runContext.render(proxyOptions.getPassword()).as(String.class).orElse(null);

        if (rUsername != null && rPassword != null) {
            Authenticator proxyAuthenticator = (route, response) -> {
                if (response.request().header("Proxy-Authorization") != null) {
                    return null;
                }
                String credential = Credentials.basic(rUsername, rPassword);
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            };
            builder.proxyAuthenticator(proxyAuthenticator);
        }
    }
}
