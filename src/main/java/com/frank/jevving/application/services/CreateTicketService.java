package com.frank.jevving.application.services;

import com.frank.jevving.application.commands.CreateTicketCommand;
import com.frank.jevving.application.usecases.CreateTicketUseCase;
import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.domain.repositories.SupportTeamRepository;
import com.frank.jevving.domain.repositories.TicketRepository;
import com.frank.jevving.domain.service.RequestAssessor;
import com.frank.jevving.domain.service.TeamDecider;
import com.frank.jevving.domain.service.UrgencyDecider;
import com.frank.jevving.domain.valueobjects.Answers;
import com.frank.jevving.domain.valueobjects.Question;
import com.frank.jevving.domain.valueobjects.Questions;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CreateTicketService implements CreateTicketUseCase {

    private final SupportTeamRepository supportTeams;
    private final TicketRepository tickets;
    private final RequestAssessor assessor;
    private final UrgencyDecider urgencyDecider;
    private final TeamDecider teamDecider;

    public CreateTicketService(SupportTeamRepository supportTeams, TicketRepository tickets,
                               RequestAssessor assessor, UrgencyDecider urgencyDecider, TeamDecider teamDecider) {
        this.supportTeams = supportTeams;
        this.tickets = tickets;
        this.assessor = assessor;
        this.urgencyDecider = urgencyDecider;
        this.teamDecider = teamDecider;
    }

    @Override
    public Uni<Ticket> create(CreateTicketCommand command) {
        return supportTeams.findAll()
                .flatMap(teams -> assessor.assess(command.request(), questionsFor(teams))
                        .map(answers -> newTicket(command, teams, answers)))
                .flatMap(tickets::create);
    }

    private static Questions questionsFor(List<SupportTeam> teams) {
        return new Questions(new Question.Urgency(), new Question.Team(teams), new Question.SeverityScore());
    }

    private Ticket newTicket(CreateTicketCommand command, List<SupportTeam> teams, Answers answers) {
        return new Ticket(command.request(), command.submittedBy())
                .urgency(urgencyDecider.decide(answers.urgency()).level())
                .severity(answers.severityScore().mostLikely())
                .dispatch(teamDecider.decide(answers.team(), teams));
    }
}
