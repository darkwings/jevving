package com.frank.jevving.domain.exceptions;

import java.util.UUID;

public class TicketNotInReviewException extends RuntimeException {

    public TicketNotInReviewException(UUID id) {
        super("Ticket is not waiting for review: " + id);
    }
}
