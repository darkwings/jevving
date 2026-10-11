package com.frank.jevving.infrastructure.persistence.repositories;

import com.frank.jevving.domain.exceptions.TicketAlreadyExistsException;
import com.frank.jevving.domain.exceptions.TicketNotFoundException;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.domain.repositories.TicketRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<UUID, Ticket> tickets = new ConcurrentHashMap<>();

    @Override
    public Uni<Ticket> create(Ticket ticket) {
        return Uni.createFrom().item(() -> {
            if (ticket.id() != null) {
                throw new IllegalStateException("Cannot create ticket with already assigned id=" + ticket.id());
            }
            ticket.id(UUID.randomUUID());
            if (tickets.putIfAbsent(ticket.id(), ticket) != null) {
                throw new TicketAlreadyExistsException(ticket.id());
            }
            return ticket;
        });
    }

    /** Emits {@code null} when no ticket has the given id. */
    @Override
    public Uni<Ticket> findById(UUID id) {
        return Uni.createFrom().item(() -> tickets.get(id));
    }

    @Override
    public Uni<Ticket> update(Ticket ticket) {
        return Uni.createFrom().item(() -> {
            if (tickets.replace(ticket.id(), ticket) == null) {
                throw new TicketNotFoundException(ticket.id());
            }
            return ticket;
        });
    }
}
