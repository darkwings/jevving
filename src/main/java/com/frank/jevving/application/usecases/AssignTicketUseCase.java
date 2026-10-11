package com.frank.jevving.application.usecases;

import com.frank.jevving.application.commands.AssignTicketCommand;
import com.frank.jevving.domain.entities.Ticket;
import io.smallrye.mutiny.Uni;

public interface AssignTicketUseCase {

    /** Assigns a ticket waiting for review to the support team picked by a human. */
    Uni<Ticket> assign(AssignTicketCommand command);
}
