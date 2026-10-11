package com.frank.jevving.domain.valueobjects;

import java.util.Map;

public sealed interface Answer {

    // JEV NOUL
    record Urgency(double probability) implements Answer {
    }

    // JEV CHOICE
    record Team(String chosenTeamId, double confidence, Map<String, Double> probabilities) implements Answer {
    }

    // JEV SCORE ANSWER RE-MAPPED
    record SeverityScore(double score, double confidence,
                         Map<Severity, Double> probabilities) implements Answer {

        /** The severity with the highest probability. */
        public Severity mostLikely() {
            return probabilities.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow(() -> new IllegalStateException("No severity probabilities"));
        }
    }
}
