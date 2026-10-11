package com.frank.jevving.infrastructure.jev;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JevAnswer.Noul.class, name = "noul"),
        @JsonSubTypes.Type(value = JevAnswer.Choice.class, name = "choice"),
        @JsonSubTypes.Type(value = JevAnswer.Score.class, name = "score")
})
public sealed interface JevAnswer {

    /** {@code noul} ranges from 0 (no) to 1 (yes). */
    record Noul(double noul) implements JevAnswer {
    }

    record Choice(String choice, double confidence, Map<String, Double> probabilities) implements JevAnswer {
    }

    /** {@code score} is probability-weighted, so it may fall between levels. */
    record Score(double score, double confidence, Map<String, String> legend,
                 Map<String, Double> probabilities) implements JevAnswer {
    }
}
