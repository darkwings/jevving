package com.frank.jevving.infrastructure.jev;

import com.frank.jevving.domain.service.RequestAssessor;
import com.frank.jevving.domain.valueobjects.Answer;
import com.frank.jevving.domain.valueobjects.Answers;
import com.frank.jevving.domain.valueobjects.Question;
import com.frank.jevving.domain.valueobjects.Questions;
import com.frank.jevving.domain.valueobjects.Severity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** {@link RequestAssessor} adapter that asks Jev all the questions in a single request. */
@ApplicationScoped
public class JevAssessor implements RequestAssessor {

    private final JevRestClient client;
    private final JevConfiguration config;

    public JevAssessor(@RestClient JevRestClient client, JevConfiguration config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Uni<Answers> assess(String request, Questions questions) {
        Map<String, JevQuestion> jevQuestions = new LinkedHashMap<>();
        jevQuestions.put(questions.urgency().questionId(), toJev(questions.urgency()));
        jevQuestions.put(questions.team().questionId(), toJev(questions.team()));
        jevQuestions.put(questions.severityScore().questionId(), toJev(questions.severityScore()));

        return client.evaluate(new JevRequest(request, config.model(), jevQuestions))
                .map(response -> new Answers(
                        toDomain(answer(response, questions.urgency(), JevAnswer.Noul.class)),
                        toDomain(answer(response, questions.team(), JevAnswer.Choice.class)),
                        toDomain(answer(response, questions.severityScore(), JevAnswer.Score.class),
                                questions.severityScore())));
    }

    // Domain → Jev

    private static JevQuestion.Noul toJev(Question.Urgency question) {
        return new JevQuestion.Noul(question.instructions());
    }

    private static JevQuestion.Choice toJev(Question.Team question) {
        return new JevQuestion.Choice(question.instructions(), question.criteria());
    }

    private static JevQuestion.Score toJev(Question.SeverityScore question) {
        return new JevQuestion.Score(question.instructions(), question.options());
    }

    // Jev → Domain

    private static Answer.Urgency toDomain(JevAnswer.Noul answer) {
        return new Answer.Urgency(answer.noul());
    }

    private static Answer.Team toDomain(JevAnswer.Choice answer) {
        return new Answer.Team(answer.choice(), answer.confidence(), answer.probabilities());
    }

    /** Jev keys score probabilities by level index, i.e. the position in {@link Question.SeverityScore#options()}. */
    private static Answer.SeverityScore toDomain(JevAnswer.Score answer, Question.SeverityScore question) {
        List<String> options = question.options();
        Map<Severity, Double> probabilities = new LinkedHashMap<>();
        answer.probabilities().forEach((level, probability) ->
                probabilities.put(Severity.valueOf(options.get(Integer.parseInt(level))), probability));
        return new Answer.SeverityScore(answer.score(), answer.confidence(), probabilities);
    }

    private static <T extends JevAnswer> T answer(JevResponse response, Question question, Class<T> type) {
        JevAnswer answer = response.answers().get(question.questionId());
        if (!type.isInstance(answer)) {
            throw new IllegalStateException("Jev returned no " + type.getSimpleName()
                    + " answer for question '" + question.questionId() + "': " + answer);
        }
        return type.cast(answer);
    }
}
