package io.kestra.plugin.slack.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.http.client.configurations.ProxyConfiguration;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.slack.app.chats.Post;

import jakarta.inject.Inject;
import okhttp3.Credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@KestraTest
class SlackProxyTest {
    // never resolved locally: the only way to reach it is through the proxy
    private static final String SLACK_URL = "http://slack.proxy-test.invalid/api/";

    @Inject
    private RunContextFactory runContextFactory;

    private FakeProxy proxy;

    @BeforeEach
    void start() throws IOException {
        proxy = new FakeProxy();
    }

    @AfterEach
    void stop() throws IOException {
        proxy.close();
    }

    @Test
    void rightCredentials() throws Exception {
        proxy.expected = Credentials.basic("user", "secret");

        run(proxyConfig("user", "secret"));

        assertThat(proxy.authHeaders).contains(Credentials.basic("user", "secret"));
        assertThat(proxy.served).isEqualTo(1);
    }

    @Test
    void wrongCredentialsDoNotLoop() {
        proxy.expected = Credentials.basic("user", "secret");

        assertThatThrownBy(() -> run(proxyConfig("user", "wrong")))
            .hasStackTraceContaining("407");

        // one challenge, one authenticated retry, then give up
        assertThat(proxy.requests).isEqualTo(2);
    }

    @Test
    void passwordWithSpecialCharacters() throws Exception {
        String password = "p:ss@word:@";
        proxy.expected = Credentials.basic("us:er@x", password);

        run(proxyConfig("us:er@x", password));

        assertThat(proxy.served).isEqualTo(1);
    }

    @Test
    void proxyWithoutAuth() throws Exception {
        run(proxyConfig(null, null));

        assertThat(proxy.served).isEqualTo(1);
        assertThat(proxy.authHeaders).isEmpty();
    }

    @Test
    void unreachableProxy() throws Exception {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }

        ProxyConfiguration config = ProxyConfiguration.builder()
            .type(Property.ofValue(Proxy.Type.HTTP))
            .address(Property.ofValue("localhost"))
            .port(Property.ofValue(closedPort))
            .build();

        assertThatThrownBy(() -> run(config)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void typeUnset() {
        ProxyConfiguration config = ProxyConfiguration.builder()
            .address(Property.ofValue("localhost"))
            .port(Property.ofValue(proxy.port()))
            .build();

        assertThatThrownBy(() -> run(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("proxy.type");
    }

    @Test
    void missingPort() {
        ProxyConfiguration config = ProxyConfiguration.builder()
            .type(Property.ofValue(Proxy.Type.HTTP))
            .address(Property.ofValue("localhost"))
            .build();

        assertThatThrownBy(() -> run(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("proxy.port");
    }

    @Test
    void portOutOfRange() {
        ProxyConfiguration config = ProxyConfiguration.builder()
            .type(Property.ofValue(Proxy.Type.HTTP))
            .address(Property.ofValue("localhost"))
            .port(Property.ofValue(70000))
            .build();

        assertThatThrownBy(() -> run(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("proxy.port");
    }

    @Test
    void usernameWithoutPassword() {
        assertThatThrownBy(() -> run(proxyConfig("user", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("proxy.password")
            .hasMessageNotContaining("user:");
    }

    @Test
    void socksWithCredentials() {
        ProxyConfiguration config = ProxyConfiguration.builder()
            .type(Property.ofValue(Proxy.Type.SOCKS))
            .address(Property.ofValue("localhost"))
            .port(Property.ofValue(proxy.port()))
            .username(Property.ofValue("user"))
            .password(Property.ofValue("topsecret"))
            .build();

        assertThatThrownBy(() -> run(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SOCKS")
            .hasMessageNotContaining("topsecret");
    }

    @Test
    void systemProxyCredentialsAreNotSentToExplicitProxy() throws Exception {
        List<String> keys = List.of("http.proxyHost", "http.proxyPort", "http.proxyUser", "http.proxyPassword");
        Map<String, String> previous = new java.util.HashMap<>();
        keys.forEach(k -> previous.put(k, System.getProperty(k)));
        try {
            System.setProperty("http.proxyHost", "localhost");
            System.setProperty("http.proxyPort", "1");
            System.setProperty("http.proxyUser", "system-user");
            System.setProperty("http.proxyPassword", "system-pass");

            run(proxyConfig(null, null));

            assertThat(proxy.served).isEqualTo(1);
            assertThat(proxy.authHeaders).isEmpty();
        } finally {
            previous.forEach((k, v) ->
            {
                if (v == null) {
                    System.clearProperty(k);
                } else {
                    System.setProperty(k, v);
                }
            });
        }
    }

    private ProxyConfiguration proxyConfig(String username, String password) {
        return ProxyConfiguration.builder()
            .type(Property.ofValue(Proxy.Type.HTTP))
            .address(Property.ofValue("localhost"))
            .port(Property.ofValue(proxy.port()))
            .username(username == null ? null : Property.ofValue(username))
            .password(password == null ? null : Property.ofValue(password))
            .build();
    }

    private void run(ProxyConfiguration config) throws Exception {
        Post task = Post.builder()
            .id(IdUtils.create())
            .type(Post.class.getName())
            .methodsEndpointUrlPrefix(SLACK_URL)
            .token(Property.ofValue("token"))
            .channel(Property.ofValue("#general"))
            .messageText(Property.ofValue("hello"))
            .proxy(config)
            .build();

        task.run(TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
    }

    /**
     * Minimal HTTP proxy: answers 407 until the expected Proxy-Authorization is received (if any is expected),
     * then returns a fake Slack response.
     */
    private static class FakeProxy implements AutoCloseable {
        private final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        private final List<String> authHeaders = new CopyOnWriteArrayList<>();
        volatile String expected;
        volatile int requests;
        volatile int served;

        FakeProxy() throws IOException {
            Thread t = new Thread(this::loop, "fake-proxy");
            t.setDaemon(true);
            t.start();
        }

        int port() {
            return server.getLocalPort();
        }

        private void loop() {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    handle(socket);
                } catch (IOException ignored) {
                    // closed or client gone
                }
            }
        }

        private void handle(Socket socket) throws IOException {
            socket.setSoTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            String line = in.readLine();
            if (line == null) {
                return;
            }
            String auth = null;
            int length = 0;
            List<String> headers = new ArrayList<>();
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                headers.add(line);
            }
            for (String h : headers) {
                String lower = h.toLowerCase();
                if (lower.startsWith("proxy-authorization:")) {
                    auth = h.substring(h.indexOf(':') + 1).trim();
                } else if (lower.startsWith("content-length:")) {
                    length = Integer.parseInt(h.substring(h.indexOf(':') + 1).trim());
                }
            }
            char[] body = new char[length];
            int read = 0;
            while (read < length) {
                int n = in.read(body, read, length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            requests++;
            if (auth != null) {
                authHeaders.add(auth);
            }

            OutputStream out = socket.getOutputStream();
            if (expected != null && !expected.equals(auth)) {
                out.write(
                    ("HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=\"test\"\r\n" +
                        "Content-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1)
                );
            } else {
                served++;
                byte[] json = "{\"ok\":true,\"ts\":\"1.1\",\"channel\":\"C1\"}".getBytes(StandardCharsets.UTF_8);
                out.write(
                    ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + json.length +
                        "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1)
                );
                out.write(json);
            }
            out.flush();
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
