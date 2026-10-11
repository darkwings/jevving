package com.frank.jevving.infrastructure.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dispatch")
public interface DispatchConfiguration {

    /** Below this confidence on the team choice, the ticket needs a human review. */
    double confidenceLimit();
}
