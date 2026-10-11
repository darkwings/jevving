package com.frank.jevving.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frank.jevving.domain.exceptions.TicketNotInReviewException;
import com.frank.jevving.domain.valueobjects.DispatchStatus;
import com.frank.jevving.domain.valueobjects.TeamDecision;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketTest {

    private static final SupportTeam DEVOPS = new SupportTeam("devops", "Infrastructure problems");
    private static final SupportTeam DEVELOPERS = new SupportTeam("developers", "Software bugs");

    @Test
    void newTicketIsNotDispatched() {
        Ticket ticket = newTicket();

        assertThat(ticket.dispatchStatus()).isNull();
        assertThat(ticket.assignedTeam()).isNull();
        assertThat(ticket.suggestedTeam()).isNull();
    }

    @Test
    void confidentDecisionAssignsTheChosenTeamWithoutSuggestion() {
        Ticket ticket = newTicket().dispatch(TeamDecision.assigned(DEVOPS, 0.9));

        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.ASSIGNED);
        assertThat(ticket.assignedTeam()).isEqualTo(DEVOPS);
        assertThat(ticket.suggestedTeam()).isNull();
    }

    @Test
    void uncertainDecisionOnlySuggestsTheChosenTeam() {
        Ticket ticket = newTicket().dispatch(TeamDecision.needsReview(DEVOPS, 0.4));

        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
        assertThat(ticket.assignedTeam()).isNull();
        assertThat(ticket.suggestedTeam()).isEqualTo(DEVOPS);
    }

    @Test
    void manualAssignmentKeepsTheSuggestion() {
        Ticket ticket = newTicket().dispatch(TeamDecision.needsReview(DEVOPS, 0.4));

        ticket.assign(DEVELOPERS);

        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.ASSIGNED);
        assertThat(ticket.assignedTeam()).isEqualTo(DEVELOPERS);
        assertThat(ticket.suggestedTeam()).isEqualTo(DEVOPS);
    }

    @Test
    void manualAssignmentRequiresReview() {
        Ticket ticket = newTicket().dispatch(TeamDecision.assigned(DEVOPS, 0.9));

        assertThatThrownBy(() -> ticket.assign(DEVELOPERS))
                .isInstanceOf(TicketNotInReviewException.class)
                .hasMessageContaining(ticket.id().toString());
        assertThat(ticket.assignedTeam()).isEqualTo(DEVOPS);
    }

    @Test
    void manualAssignmentIsNotAllowedBeforeDispatch() {
        Ticket ticket = newTicket();

        assertThatThrownBy(() -> ticket.assign(DEVELOPERS)).isInstanceOf(TicketNotInReviewException.class);
    }

    @Test
    void manualAssignmentRequiresATeam() {
        Ticket ticket = newTicket().dispatch(TeamDecision.needsReview(DEVOPS, 0.4));

        assertThatThrownBy(() -> ticket.assign(null)).isInstanceOf(NullPointerException.class);
        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
    }

    @Test
    void decisionRequiresAChosenTeam() {
        assertThatThrownBy(() -> TeamDecision.needsReview(null, 0.4)).isInstanceOf(NullPointerException.class);
    }

    private static Ticket newTicket() {
        return new Ticket("Something is off", "alice").id(UUID.randomUUID());
    }
}
