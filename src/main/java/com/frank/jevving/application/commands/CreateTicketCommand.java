package com.frank.jevving.application.commands;

/**
 * Request to open a support ticket.
 *
 * @param request     the text written by the customer
 * @param submittedBy the id of the user opening the ticket
 */
public record CreateTicketCommand(String request, String submittedBy) {
}
