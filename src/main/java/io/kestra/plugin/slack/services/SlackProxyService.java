package io.kestra.plugin.slack.services;

import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

/**
 * Applies an already rendered and validated proxy to an OkHttp client builder. Never logs the credentials.
 */
public final class SlackProxyService {
    private SlackProxyService() {
    }

    /**
     * @param username Basic proxy username, only used (with the password) for HTTP proxies
     * @param password Basic proxy password
     */
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
        }
    }

    /**
     * Same as {@link #apply} but guarantees no authenticator is used when there are no credentials.
     */
    public static void applyExplicit(OkHttpClient.Builder builder, Proxy.Type type, String host, int port, String username, String password) {
        apply(builder, type, host, port, username, password);
        if (username == null || password == null) {
            builder.proxyAuthenticator(Authenticator.NONE);
        }
    }
}
