package com.frank.jevving.domain.valueobjects;

/**
 * The thresholds the domain needs to turn the assessor's probabilities into decisions.
 *
 * @param urgencyLimit    a ticket is urgent when the urgency probability is above this value
 * @param confidenceLimit below this confidence on the team choice, the ticket needs a human review
 */
public record DecisionThresholds(double urgencyLimit, double confidenceLimit) {
}
