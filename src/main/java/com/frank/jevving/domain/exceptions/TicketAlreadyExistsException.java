package com.frank.jevving.domain.exceptions;

import java.util.UUID;

public class TicketAlreadyExistsException extends RuntimeException {

    public TicketAlreadyExistsException(UUID id) {
        super("Ticket already exists: " + id);
    }
}
