package com.frank.jevving.infrastructure.config;

import com.frank.jevving.domain.valueobjects.DecisionThresholds;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/** Reads the decision thresholds from the configuration and provides them to the domain. */
public class DecisionThresholdsProducer {

    @Produces
    @Singleton
    DecisionThresholds decisionThresholds(UrgencyConfiguration urgency, DispatchConfiguration dispatch) {
        return new DecisionThresholds(urgency.limit(), dispatch.confidenceLimit());
    }
}
