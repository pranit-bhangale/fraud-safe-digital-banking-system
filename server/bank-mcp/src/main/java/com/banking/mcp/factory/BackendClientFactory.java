package com.banking.mcp.factory;

import com.banking.mcp.exception.BackendException;
import com.banking.mcp.properties.BackendProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class BackendClientFactory {

    private final RestClient.Builder restClientBuilder;
    private final BackendProperties properties;
    private final List<CloseableHttpClient> httpClients = new CopyOnWriteArrayList<>();

    public <T> T create(String name, Class<T> apiType) {
        var cfg = Optional.ofNullable(properties.clients().get(name))
                .orElseThrow(() -> new IllegalStateException("Missing backend.clients." + name));
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(cfg.maxConnections())
                .setMaxConnPerRoute(cfg.maxPerRoute())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(cfg.connectTimeout()))
                        .setSocketTimeout(Timeout.of(cfg.readTimeout()))
                        .setTimeToLive(TimeValue.of(cfg.connectionTtl()))
                        .build())
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.of(cfg.connectionRequestTimeout()))
                        .setResponseTimeout(Timeout.of(cfg.readTimeout()))
                        .build())
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(cfg.idleEviction()))
                .disableCookieManagement()
                .disableRedirectHandling()
                .build();
        httpClients.add(httpClient);
        RestClient client = restClientBuilder.clone()
                .baseUrl(cfg.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                    throw new BackendException(name, res.getStatusCode().value());
                })
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(apiType);
    }

    @PreDestroy
    void shutdown() {
        httpClients.forEach(c -> {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        });
    }
}