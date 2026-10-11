package com.frank.jevving.domain.entities;

import com.frank.jevving.domain.exceptions.TicketNotInReviewException;
import com.frank.jevving.domain.valueobjects.DispatchStatus;
import com.frank.jevving.domain.valueobjects.Severity;
import com.frank.jevving.domain.valueobjects.TeamDecision;
import com.frank.jevving.domain.valueobjects.UrgencyLevel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.Objects;
import java.util.UUID;

/**
 * A support ticket.
 * <p>
 * Dispatch state only changes through {@link #dispatch(TeamDecision)} and {@link #assign(SupportTeam)}, which keep
 * these invariants once the ticket is dispatched:
 * <ul>
 *     <li>{@link DispatchStatus#NEEDS_REVIEW}: {@code assignedTeam} is {@code null} and
 *     {@code suggestedTeam} is not;</li>
 *     <li>{@link DispatchStatus#ASSIGNED}: {@code assignedTeam} is not {@code null}, and
 *     {@code suggestedTeam} is set only if the ticket was assigned manually after a review.</li>
 * </ul>
 */
@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ticket {

    @Setter
    UUID id;

    final String request;
    final String submittedBy;

    DispatchStatus dispatchStatus;

    SupportTeam assignedTeam;

    /** The team chosen by the assessor, set only while the ticket needs review (and kept once a human assigns it). */
    SupportTeam suggestedTeam;

    @Setter
    UrgencyLevel urgency;

    @Setter
    Severity severity;

    public Ticket(String request, String submittedBy) {
        this.request = request;
        this.submittedBy = submittedBy;
    }

    /** Applies the assessor's decision: assigns the chosen team or sends the ticket to human review. */
    public Ticket dispatch(TeamDecision decision) {
        SupportTeam chosen = decision.chosenTeam();
        dispatchStatus = decision.status();
        assignedTeam = switch (decision.status()) {
            case ASSIGNED -> chosen;
            case NEEDS_REVIEW -> null;
        };
        suggestedTeam = switch (decision.status()) {
            case ASSIGNED -> null;
            case NEEDS_REVIEW -> chosen;
        };
        return this;
    }

    /**
     * Manually assigns a ticket waiting for review. The assessor's suggestion is kept.
     *
     * @throws TicketNotInReviewException if the ticket does not need review
     */
    public Ticket assign(SupportTeam team) {
        Objects.requireNonNull(team, "team");
        if (dispatchStatus != DispatchStatus.NEEDS_REVIEW) {
            throw new TicketNotInReviewException(id);
        }
        dispatchStatus = DispatchStatus.ASSIGNED;
        assignedTeam = team;
        return this;
    }
}
