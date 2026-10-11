package com.frank.jevving.infrastructure.jev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.Answers;
import com.frank.jevving.domain.valueobjects.Question;
import com.frank.jevving.domain.valueobjects.Questions;
import com.frank.jevving.domain.valueobjects.Severity;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JevAssessorTest {

    private static final JevConfiguration CONFIG = new JevConfiguration() {
        @Override
        public String apiKey() {
            return "key";
        }

        @Override
        public String baseUrl() {
            return "http://localhost";
        }

        @Override
        public String model() {
            return "jev-latest";
        }
    };

    private final Questions questions = new Questions(
            new Question.Urgency(),
            new Question.Team(List.of(
                    new SupportTeam("devops", "Infrastructure problems"),
                    new SupportTeam("developers", "Software bugs"))),
            new Question.SeverityScore());

    @Test
    void mapsDomainQuestionsToJevRequest() {
        AtomicReference<JevRequest> sent = new AtomicReference<>();
        JevAssessor assessor = new JevAssessor(request -> {
            sent.set(request);
            return Uni.createFrom().item(validResponse());
        }, CONFIG);

        assessor.assess("Production is down!", questions).await().indefinitely();

        JevRequest request = sent.get();
        assertThat(request.state()).isEqualTo("Production is down!");
        assertThat(request.model()).isEqualTo("jev-latest");
        assertThat(request.questions()).containsOnlyKeys("urgent", "team", "severity");
        assertThat(request.questions().get("urgent"))
                .isEqualTo(new JevQuestion.Noul("Does the customer's message convey a need for immediate help?"));
        assertThat(request.questions().get("team")).isEqualTo(new JevQuestion.Choice(
                "Which team is best placed to resolve this request?",
                Map.of("devops", "Infrastructure problems", "developers", "Software bugs",
                        "unclear", "The request is too vague or ambiguous to tell which team should handle it")));
        assertThat(request.questions().get("severity")).isEqualTo(new JevQuestion.Score(
                "How serious is the problem the customer reports?", List.of("MINOR", "MAJOR", "BLOCKER")));
    }

    @Test
    void mapsJevResponseToDomainAnswers() {
        JevAssessor assessor = new JevAssessor(request -> Uni.createFrom().item(validResponse()), CONFIG);

        Answers answers = assessor.assess("Production is down!", questions).await().indefinitely();

        assertThat(answers.urgency()).isEqualTo(new Answer.Urgency(0.95));
        assertThat(answers.team()).isEqualTo(new Answer.Team("devops", 0.8,
                Map.of("devops", 0.85, "developers", 0.15)));
        assertThat(answers.severityScore().score()).isEqualTo(1.7);
        assertThat(answers.severityScore().confidence()).isEqualTo(0.7);
        assertThat(answers.severityScore().probabilities()).containsOnly(
                entry(Severity.BLOCKER, 0.75),
                entry(Severity.MAJOR, 0.2),
                entry(Severity.MINOR, 0.05));
    }

    @Test
    void failsWhenAnAnswerIsMissing() {
        JevAssessor assessor = new JevAssessor(request -> Uni.createFrom().item(new JevResponse("jev-1.13.0",
                Map.of("urgent", new JevAnswer.Noul(0.1)), null)), CONFIG);

        assertThatThrownBy(() -> assessor.assess("anything", questions).await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'team'");
    }

    private static JevResponse validResponse() {
        return new JevResponse("jev-1.13.0", Map.of(
                "urgent", new JevAnswer.Noul(0.95),
                "team", new JevAnswer.Choice("devops", 0.8, Map.of("devops", 0.85, "developers", 0.15)),
                "severity", new JevAnswer.Score(1.7, 0.7,
                        Map.of("0", "MINOR", "1", "MAJOR", "2", "BLOCKER"),
                        Map.of("0", 0.05, "1", 0.2, "2", 0.75))),
                new JevResponse.Usage(40, 3));
    }
}
