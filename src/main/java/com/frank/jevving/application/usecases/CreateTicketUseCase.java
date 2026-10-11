package com.frank.jevving.application.usecases;

import com.frank.jevving.application.commands.CreateTicketCommand;
import com.frank.jevving.domain.entities.Ticket;
import io.smallrye.mutiny.Uni;

public interface CreateTicketUseCase {

    /** Assesses the request, dispatches it to a support team and stores the resulting ticket. */
    Uni<Ticket> create(CreateTicketCommand command);
}
