package com.frank.jevving.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frank.jevving.application.commands.AssignTicketCommand;
import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.domain.exceptions.SupportTeamNotFoundException;
import com.frank.jevving.domain.exceptions.TicketNotFoundException;
import com.frank.jevving.domain.exceptions.TicketNotInReviewException;
import com.frank.jevving.domain.repositories.SupportTeamRepository;
import com.frank.jevving.domain.valueobjects.DispatchStatus;
import com.frank.jevving.domain.valueobjects.TeamDecision;
import com.frank.jevving.infrastructure.persistence.repositories.InMemoryTicketRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssignTicketServiceTest {

    private static final SupportTeam DEVOPS = new SupportTeam("devops", "Infrastructure problems");
    private static final SupportTeam DEVELOPERS = new SupportTeam("developers", "Software bugs");
    private static final SupportTeamRepository TEAMS = () -> Uni.createFrom().item(List.of(DEVOPS, DEVELOPERS));

    InMemoryTicketRepository tickets;
    AssignTicketService service;

    @BeforeEach
    void setUp() {
        tickets = new InMemoryTicketRepository();
        service = new AssignTicketService(tickets, TEAMS);
    }

    @Test
    void assignsTicketWaitingForReview() {
        Ticket stored = store(TeamDecision.needsReview(DEVOPS, 0.4));

        Ticket assigned = service.assign(new AssignTicketCommand(stored.id(), "developers"))
                .await().indefinitely();

        assertThat(assigned.dispatchStatus()).isEqualTo(DispatchStatus.ASSIGNED);
        assertThat(assigned.assignedTeam()).isEqualTo(DEVELOPERS);
        assertThat(assigned.suggestedTeam()).isEqualTo(DEVOPS);
        assertThat(tickets.findById(stored.id()).await().indefinitely().assignedTeam())
                .isEqualTo(DEVELOPERS);
    }

    @Test
    void failsWhenTicketIsMissing() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.assign(new AssignTicketCommand(id, "devops")).await().indefinitely())
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void failsWhenTeamIsUnknown() {
        Ticket stored = store(TeamDecision.needsReview(DEVOPS, 0.4));

        assertThatThrownBy(() -> service.assign(new AssignTicketCommand(stored.id(), "marketing"))
                .await().indefinitely())
                .isInstanceOf(SupportTeamNotFoundException.class)
                .hasMessageContaining("marketing");
        assertThat(stored.dispatchStatus()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
    }

    @Test
    void failsWhenTicketIsAlreadyAssigned() {
        Ticket stored = store(TeamDecision.assigned(DEVOPS, 0.9));

        assertThatThrownBy(() -> service.assign(new AssignTicketCommand(stored.id(), "developers"))
                .await().indefinitely())
                .isInstanceOf(TicketNotInReviewException.class);
        assertThat(stored.assignedTeam()).isEqualTo(DEVOPS);
    }

    private Ticket store(TeamDecision decision) {
        return tickets.create(new Ticket("Something is off", "alice").dispatch(decision)).await().indefinitely();
    }
}
