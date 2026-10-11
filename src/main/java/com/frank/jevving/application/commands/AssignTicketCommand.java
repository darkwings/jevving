package com.frank.jevving.application.commands;

import java.util.UUID;

/**
 * Human decision on a ticket waiting for review.
 *
 * @param ticketId        the ticket to assign
 * @param teamId the team picked by the reviewer
 */
public record AssignTicketCommand(UUID ticketId, String teamId) {
}
