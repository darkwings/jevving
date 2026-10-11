package com.frank.jevving.infrastructure.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "urgency")
public interface UrgencyConfiguration {

    /** A ticket is urgent when the assessor's urgency probability is above this value. */
    double limit();
}
