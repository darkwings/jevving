package com.frank.jevving.domain.service;

import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.UrgencyDecision;
import com.frank.jevving.domain.valueobjects.UrgencyLevel;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UrgencyDecider {

    private final double limit;

    public UrgencyDecider(@ConfigProperty(name = "urgency.limit") double limit) {
        this.limit = limit;
    }

    public UrgencyDecision decide(Answer.Urgency urgency) {

        UrgencyLevel level = urgency.probability() > limit ? UrgencyLevel.URGENT : UrgencyLevel.NORMAL;
        return new UrgencyDecision(level, urgency.probability());
    }
}
