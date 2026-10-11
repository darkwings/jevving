package com.frank.jevving.domain.service;

import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.exceptions.InvalidAssessmentException;
import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.Question;
import com.frank.jevving.domain.valueobjects.TeamDecision;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TeamDecider {

    private final double limit;

    public TeamDecider(@ConfigProperty(name = "dispatch.confidence-limit") double limit) {
        this.limit = limit;
    }

    /**
     * Assigns the chosen team when the assessor is confident enough, otherwise asks for a human review.
     * <p>
     * A request the assessor finds {@link Question.Team#UNCLEAR_ID unclear} always needs review, whatever the
     * confidence; the suggestion is then the most probable real team.
     *
     * @throws InvalidAssessmentException if the chosen team is not among {@code teams}, or the assessor found the
     *                                    request unclear without ranking any team
     */
    public TeamDecision decide(Answer.Team answer, List<SupportTeam> teams) {
        if (Question.Team.UNCLEAR_ID.equals(answer.chosenTeamId())) {
            SupportTeam leaning = mostProbable(answer.probabilities(), teams)
                    .orElseThrow(() -> new InvalidAssessmentException(
                            "Assessor found the request unclear without ranking any support team"));
            return TeamDecision.needsReview(leaning, answer.confidence());
        }

        SupportTeam chosen = teams.stream()
                .filter(team -> team.id().equals(answer.chosenTeamId()))
                .findFirst()
                .orElseThrow(() -> new InvalidAssessmentException(
                        "Assessor chose an unknown support team: " + answer.chosenTeamId()));

        return answer.confidence() >= limit
                ? TeamDecision.assigned(chosen, answer.confidence())
                : TeamDecision.needsReview(chosen, answer.confidence());
    }

    private static Optional<SupportTeam> mostProbable(Map<String, Double> probabilities,
                                                                List<SupportTeam> teams) {
        return teams.stream()
                .filter(team -> probabilities.containsKey(team.id()))
                .max(Comparator.comparingDouble(team -> probabilities.get(team.id())));
    }
}
