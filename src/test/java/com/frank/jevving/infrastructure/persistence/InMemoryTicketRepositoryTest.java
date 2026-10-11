package com.frank.jevving.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frank.jevving.domain.exceptions.TicketNotFoundException;
import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.domain.valueobjects.TeamDecision;
import java.util.UUID;

import com.frank.jevving.infrastructure.persistence.repositories.InMemoryTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryTicketRepositoryTest {

    InMemoryTicketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTicketRepository();
    }

    @Test
    void createAssignsIdAndTicketCanBeFoundById() {
        Ticket ticket = newTicket();

        Ticket created = repository.create(ticket).await().indefinitely();
        Ticket found = repository.findById(created.id()).await().indefinitely();

        assertThat(created).isSameAs(ticket);
        assertThat(created.id()).isNotNull();
        assertThat(found).isSameAs(ticket);
    }

    @Test
    void findByIdEmitsNullWhenTicketIsMissing() {
        Ticket found = repository.findById(UUID.randomUUID()).await().indefinitely();

        assertThat(found).isNull();
    }

    @Test
    void createFailsWhenTicketAlreadyHasId() {
        Ticket ticket = newTicket().id(UUID.randomUUID());

        assertThatThrownBy(() -> repository.create(ticket).await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ticket.id().toString());
        assertThat(repository.findById(ticket.id()).await().indefinitely()).isNull();
    }

    @Test
    void updateReplacesStoredTicket() {
        Ticket original = newTicket();
        repository.create(original).await().indefinitely();
        Ticket modified = new Ticket(original.request(), original.submittedBy()).id(original.id());
        modified.dispatch(TeamDecision.assigned(new SupportTeam("Support", "triage"), 0.9));

        repository.update(modified).await().indefinitely();

        Ticket found = repository.findById(original.id()).await().indefinitely();
        assertThat(found).isSameAs(modified);
        assertThat(found.assignedTeam()).isEqualTo(new SupportTeam("Support", "triage"));
    }

    @Test
    void updateFailsWhenTicketIsMissing() {
        Ticket ticket = newTicket().id(UUID.randomUUID());

        assertThatThrownBy(() -> repository.update(ticket).await().indefinitely())
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining(ticket.id().toString());
        assertThat(repository.findById(ticket.id()).await().indefinitely()).isNull();
    }

    private static Ticket newTicket() {
        return new Ticket("Printer is broken", "alice");
    }
}
