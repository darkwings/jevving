package com.frank.jevving.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.exceptions.InvalidAssessmentException;
import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.DispatchStatus;
import com.frank.jevving.domain.valueobjects.TeamDecision;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TeamDeciderTest {

    private static final SupportTeam DEVOPS = new SupportTeam("devops", "Infrastructure problems");
    private static final List<SupportTeam> TEAMS =
            List.of(DEVOPS, new SupportTeam("developers", "Software bugs"));

    private final TeamDecider decider = new TeamDecider(0.7);

    @Test
    void assignsChosenTeamWhenConfident() {
        TeamDecision decision = decider.decide(answer("devops", 0.9), TEAMS);

        assertThat(decision).isEqualTo(new TeamDecision(DispatchStatus.ASSIGNED, DEVOPS, 0.9));
    }

    @Test
    void assignsChosenTeamWhenConfidenceEqualsTheLimit() {
        TeamDecision decision = decider.decide(answer("devops", 0.7), TEAMS);

        assertThat(decision.status()).isEqualTo(DispatchStatus.ASSIGNED);
    }

    @Test
    void needsReviewBelowTheLimit() {
        TeamDecision decision = decider.decide(answer("devops", 0.4), TEAMS);

        assertThat(decision.status()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
        assertThat(decision.chosenTeam()).isEqualTo(DEVOPS);
        assertThat(decision.confidence()).isEqualTo(0.4);
    }

    @Test
    void failsWhenChosenTeamIsUnknown() {
        assertThatThrownBy(() -> decider.decide(answer("marketing", 0.9), TEAMS))
                .isInstanceOf(InvalidAssessmentException.class)
                .hasMessageContaining("marketing");
    }

    @Test
    void failsWhenChosenTeamIsUnknownEvenIfNotConfident() {
        assertThatThrownBy(() -> decider.decide(answer("marketing", 0.1), TEAMS))
                .isInstanceOf(InvalidAssessmentException.class);
    }

    @Test
    void unclearRequestNeedsReviewEvenWhenConfident() {
        Answer.Team answer = new Answer.Team("unclear", 0.99,
                Map.of("unclear", 0.99, "devops", 0.004, "developers", 0.006));

        TeamDecision decision = decider.decide(answer, TEAMS);

        assertThat(decision.status()).isEqualTo(DispatchStatus.NEEDS_REVIEW);
        assertThat(decision.chosenTeam()).isEqualTo(TEAMS.get(1));
        assertThat(decision.confidence()).isEqualTo(0.99);
    }

    @Test
    void unclearRequestSuggestsTheMostProbableRealTeam() {
        Answer.Team answer = new Answer.Team("unclear", 0.6,
                Map.of("unclear", 0.6, "devops", 0.3, "developers", 0.1));

        TeamDecision decision = decider.decide(answer, TEAMS);

        assertThat(decision.chosenTeam()).isEqualTo(DEVOPS);
    }

    @Test
    void unclearRequestWithoutRankedTeamsIsInvalid() {
        Answer.Team answer = new Answer.Team("unclear", 0.9, Map.of("unclear", 0.9));

        assertThatThrownBy(() -> decider.decide(answer, TEAMS))
                .isInstanceOf(InvalidAssessmentException.class)
                .hasMessageContaining("unclear");
    }

    private static Answer.Team answer(String teamId, double confidence) {
        return new Answer.Team(teamId, confidence, Map.of(teamId, confidence));
    }
}
