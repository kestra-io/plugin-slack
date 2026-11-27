package io.kestra.plugin.slack.services;

import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

public final class SlackProxyService {
    private SlackProxyService() {
    }

    public static void apply(OkHttpClient.Builder builder, Proxy.Type type, String host, int port, String username, String password) {
        builder.proxy(new Proxy(type, new InetSocketAddress(host, port)));

        if (username != null && password != null) {
            String credential = Credentials.basic(username, password);
            builder.proxyAuthenticator((route, response) ->
            {
                // already answered once with credentials: give up instead of looping on a 407
                if (response.request().header("Proxy-Authorization") != null) {
                    return null;
                }
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            });
        } else {
            // a builder derived from the SDK client may carry system-proxy credentials
            builder.proxyAuthenticator(Authenticator.NONE);
        }
    }
}
