package com.frank.jevving.presentation.http.mappers;

import com.frank.jevving.application.commands.AssignTicketCommand;
import com.frank.jevving.application.commands.CreateTicketCommand;
import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.presentation.http.dto.AssignTicketRequest;
import com.frank.jevving.presentation.http.dto.CreateTicketRequest;
import com.frank.jevving.presentation.http.dto.SupportTeamResponse;
import com.frank.jevving.presentation.http.dto.TicketResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/** Maps between the HTTP DTOs and the application/domain types. */
@ApplicationScoped
public class TicketHttpMapper {

    public CreateTicketCommand toCommand(CreateTicketRequest request) {
        return new CreateTicketCommand(request.request(), request.submittedBy());
    }

    public AssignTicketCommand toCommand(UUID ticketId, AssignTicketRequest request) {
        return new AssignTicketCommand(ticketId, request.teamId());
    }

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.id(),
                ticket.request(),
                ticket.submittedBy(),
                ticket.urgency() == null ? null : ticket.urgency().name(),
                ticket.severity() == null ? null : ticket.severity().name(),
                ticket.dispatchStatus() == null ? null : ticket.dispatchStatus().name(),
                toResponse(ticket.assignedTeam()),
                toResponse(ticket.suggestedTeam()));
    }

    private static SupportTeamResponse toResponse(SupportTeam team) {
        return team == null ? null : new SupportTeamResponse(team.id(), team.capability());
    }
}
