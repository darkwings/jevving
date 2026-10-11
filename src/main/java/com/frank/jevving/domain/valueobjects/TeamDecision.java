package com.frank.jevving.domain.valueobjects;

import com.frank.jevving.domain.entities.SupportTeam;

import java.util.Objects;

/**
 * Outcome of the dispatch of a ticket.
 *
 * @param status     whether the chosen team is assigned or only suggested to a human reviewer
 * @param chosenTeam the team chosen by the assessor
 * @param confidence the confidence of the assessor in its choice
 */
public record TeamDecision(DispatchStatus status, SupportTeam chosenTeam, double confidence) {

    public TeamDecision {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(chosenTeam, "chosenTeam");
    }

    public static TeamDecision assigned(SupportTeam team, double confidence) {
        return new TeamDecision(DispatchStatus.ASSIGNED, team, confidence);
    }

    public static TeamDecision needsReview(SupportTeam suggestedTeam, double confidence) {
        return new TeamDecision(DispatchStatus.NEEDS_REVIEW, suggestedTeam, confidence);
    }
}
