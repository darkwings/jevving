package com.frank.jevving.application.services;

import com.frank.jevving.application.commands.AssignTicketCommand;
import com.frank.jevving.application.usecases.AssignTicketUseCase;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.domain.exceptions.SupportTeamNotFoundException;
import com.frank.jevving.domain.exceptions.TicketNotFoundException;
import com.frank.jevving.domain.repositories.SupportTeamRepository;
import com.frank.jevving.domain.repositories.TicketRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AssignTicketService implements AssignTicketUseCase {

    private final TicketRepository tickets;
    private final SupportTeamRepository supportTeams;

    public AssignTicketService(TicketRepository tickets, SupportTeamRepository supportTeams) {
        this.tickets = tickets;
        this.supportTeams = supportTeams;
    }

    @Override
    public Uni<Ticket> assign(AssignTicketCommand command) {
        return tickets.findById(command.ticketId())
                .onItem().ifNull().failWith(() -> new TicketNotFoundException(command.ticketId()))
                .flatMap(ticket -> supportTeams.findById(command.teamId())
                        .onItem().ifNull().failWith(() -> new SupportTeamNotFoundException(command.teamId()))
                        .map(ticket::assign))
                .flatMap(tickets::update);
    }
}
