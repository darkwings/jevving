package com.frank.jevving.infrastructure.jev;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

public class WireMockJevResource implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer server;

    public static WireMockServer server() {
        return server;
    }

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        return Map.of(
                "jev.base-url", server.baseUrl(),
                "jev.api-key", "test-key");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
