package com.frank.jevving.domain.service;

import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.DecisionThresholds;
import com.frank.jevving.domain.valueobjects.UrgencyDecision;
import com.frank.jevving.domain.valueobjects.UrgencyLevel;
import jakarta.enterprise.context.ApplicationScoped;

/** Turns the assessor's urgency probability into a decision: urgent when it is above the limit. */
@ApplicationScoped
public class UrgencyDecider {

    private final double limit;

    public UrgencyDecider(DecisionThresholds thresholds) {
        this.limit = thresholds.urgencyLimit();
    }

    public UrgencyDecision decide(Answer.Urgency urgency) {

        UrgencyLevel level = urgency.probability() > limit ? UrgencyLevel.URGENT : UrgencyLevel.NORMAL;
        return new UrgencyDecision(level, urgency.probability());
    }
}
