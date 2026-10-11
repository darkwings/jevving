package com.frank.jevving.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frank.jevving.application.commands.CreateTicketCommand;
import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.entities.Ticket;
import com.frank.jevving.domain.exceptions.InvalidAssessmentException;
import com.frank.jevving.domain.repositories.SupportTeamRepository;
import com.frank.jevving.domain.service.RequestAssessor;
import com.frank.jevving.domain.service.TeamDecider;
import com.frank.jevving.domain.service.UrgencyDecider;
import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.Answers;
import com.frank.jevving.domain.valueobjects.DispatchStatus;
import com.frank.jevving.domain.valueobjects.Questions;
import com.frank.jevving.domain.valueobjects.Severity;
import com.frank.jevving.domain.valueobjects.UrgencyLevel;
import com.frank.jevving.infrastructure.persistence.repositories.InMemoryTicketRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTicketServiceTest {

    private static final SupportTeam DEVOPS = new SupportTeam("devops", "Infrastructure problems");
    private static final SupportTeam DEVELOPERS = new SupportTeam("developers", "Software bugs");
    private static final SupportTeamRepository TEAMS = () -> Uni.createFrom().item(List.of(DEVOPS, DEVELOPERS));

    InMemoryTicketRepository tickets;
    AtomicReference<Questions> askedQuestions;

    @BeforeEach
    void setUp() {
        tickets = new InMemoryTicketRepository();
        askedQuestions = new AtomicReference<>();
    }

    @Test
    void createsTicketFromAssessment() {
        CreateTicketService service = service(answers(0.95, "devops"));

        Ticket ticket = service.create(new CreateTicketCommand("Production is down!", "alice"))
                .await().indefinitely();

        assertThat(ticket.id()).isNotNull();
        assertThat(ticket.request()).isEqualTo("Production is down!");
        assertThat(ticket.submittedBy()).isEqualTo("alice");
        assertThat(ticket.urgency()).isEqualTo(UrgencyLevel.URGENT);
        assertThat(ticket.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.ASSIGNED);
        assertThat(ticket.assignedTeam()).isEqualTo(DEVOPS);
        assertThat(ticket.suggestedTeam()).isNull();
        assertThat(tickets.findById(ticket.id()).await().indefinitely()).isSameAs(ticket);
    }

    @Test
    void ticketNeedsReviewWhenAssessorIsNotConfidentAboutTheTeam() {
        CreateTicketService service = service(answers(0.95, "devops", 0.4));

        Ticket ticket = service.create(new CreateTicketCommand("Something is off", "alice"))
                .await().indefinitely();

        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
        assertThat(ticket.assignedTeam()).isNull();
        assertThat(ticket.suggestedTeam()).isEqualTo(DEVOPS);
        assertThat(ticket.urgency()).isEqualTo(UrgencyLevel.URGENT);
        assertThat(ticket.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(tickets.findById(ticket.id()).await().indefinitely()).isSameAs(ticket);
    }

    @Test
    void ticketNeedsReviewWhenAssessorFindsTheRequestUnclear() {
        Answers unclear = new Answers(
                new Answer.Urgency(0.2),
                new Answer.Team("unclear", 0.95, Map.of("unclear", 0.95, "devops", 0.03, "developers", 0.02)),
                new Answer.SeverityScore(0.5, 0.6, Map.of(Severity.MINOR, 0.5, Severity.MAJOR, 0.4,
                        Severity.BLOCKER, 0.1)));
        CreateTicketService service = service(unclear);

        Ticket ticket = service.create(new CreateTicketCommand("Something is not working", "alice"))
                .await().indefinitely();

        assertThat(ticket.dispatchStatus()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
        assertThat(ticket.assignedTeam()).isNull();
        assertThat(ticket.suggestedTeam()).isEqualTo(DEVOPS);
    }

    @Test
    void asksAboutAllAvailableTeams() {
        CreateTicketService service = service(answers(0.95, "devops"));

        service.create(new CreateTicketCommand("Production is down!", "alice")).await().indefinitely();

        assertThat(askedQuestions.get().team().criteria())
                .containsOnlyKeys("devops", "developers", "unclear");
    }

    @Test
    void ticketIsNotUrgentBelowTheLimit() {
        CreateTicketService service = service(answers(0.5, "developers"));

        Ticket ticket = service.create(new CreateTicketCommand("Typo on the home page", "bob"))
                .await().indefinitely();

        assertThat(ticket.urgency()).isEqualTo(UrgencyLevel.NORMAL);
        assertThat(ticket.assignedTeam()).isEqualTo(DEVELOPERS);
    }

    @Test
    void failsWhenAssessorPicksAnUnknownTeam() {
        CreateTicketService service = service(answers(0.95, "marketing"));

        assertThatThrownBy(() -> service.create(new CreateTicketCommand("Help", "alice")).await().indefinitely())
                .isInstanceOf(InvalidAssessmentException.class)
                .hasMessageContaining("marketing");
    }

    private CreateTicketService service(Answers answers) {
        RequestAssessor assessor = (request, questions) -> {
            askedQuestions.set(questions);
            return Uni.createFrom().item(answers);
        };
        return new CreateTicketService(TEAMS, tickets, assessor, new UrgencyDecider(0.9), new TeamDecider(0.7));
    }

    private static Answers answers(double urgency, String teamId) {
        return answers(urgency, teamId, 0.8);
    }

    private static Answers answers(double urgency, String teamId, double teamConfidence) {
        return new Answers(
                new Answer.Urgency(urgency),
                new Answer.Team(teamId, teamConfidence, Map.of(teamId, teamConfidence)),
                new Answer.SeverityScore(1.7, 0.7,
                        Map.of(Severity.MINOR, 0.05, Severity.MAJOR, 0.2, Severity.BLOCKER, 0.75)));
    }
}
