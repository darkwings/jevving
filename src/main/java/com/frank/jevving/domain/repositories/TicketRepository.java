package com.frank.jevving.domain.repositories;


import com.frank.jevving.domain.entities.Ticket;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface TicketRepository {

    Uni<Ticket> create(Ticket ticket);

    Uni<Ticket> findById(UUID id);

    Uni<Ticket> update(Ticket ticket);
}
