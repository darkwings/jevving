package com.frank.jevving.infrastructure.jev;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "jev")
public interface JevConfiguration {

    String apiKey();

    String baseUrl();

    String model();
}
