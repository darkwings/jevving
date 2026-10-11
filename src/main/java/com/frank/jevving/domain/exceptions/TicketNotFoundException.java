package com.frank.jevving.domain.exceptions;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(UUID id) {
        super("Ticket not found: " + id);
    }
}
