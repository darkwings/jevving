package com.frank.jevving.infrastructure.jev;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

@ApplicationScoped
public class JevAuthFilter implements ClientRequestFilter {

    @Inject
    JevConfiguration config;

    @Override
    public void filter(ClientRequestContext context) {
        context.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey());
    }
}
