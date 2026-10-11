package com.frank.jevving.domain.valueobjects;

import com.frank.jevving.domain.entities.SupportTeam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public sealed interface Question {

    String questionId();

    String instructions();

    // JEV NOUL
    final class Urgency implements Question {

        @Override
        public String questionId() {
            return "urgent";
        }

        @Override
        public String instructions() {
            return "Does the customer's message convey a need for immediate help?";
        }
    }

    // JEV CHOICE
    final class Team implements Question {

        /** Id of the option the assessor picks when the request is too vague to choose a team. */
        public static final String UNCLEAR_ID = "unclear";

        static final String UNCLEAR_DESCRIPTION =
                "The request is too vague or ambiguous to tell which team should handle it";

        List<SupportTeam> teams;

        public Team(List<SupportTeam> teams) {
            this.teams = teams;
        }

        @Override
        public String questionId() {
            return "team";
        }

        @Override
        public String instructions() {
            return "Which team is best placed to resolve this request?";
        }

        /**
         * Team id → capability, in the order of the given teams, followed by the {@link #UNCLEAR_ID} option that
         * lets the assessor say the request is too vague instead of guessing a team.
         */
        // TODO: troppo vicino a JEV, rivedere
        public Map<String, String> criteria() {
            Map<String, String> criteria = new LinkedHashMap<>();
            for (SupportTeam team : teams) {
                if (criteria.putIfAbsent(team.id(), team.capability()) != null) {
                    throw new IllegalStateException("Duplicate support team id: " + team.id());
                }
            }
            if (criteria.putIfAbsent(UNCLEAR_ID, UNCLEAR_DESCRIPTION) != null) {
                throw new IllegalStateException("Support team id reserved for unclear requests: " + UNCLEAR_ID);
            }
            return criteria;
        }
    }

    // JEV SCORE
    final class SeverityScore implements Question {

        @Override
        public String questionId() {
            return "severity";
        }

        @Override
        public String instructions() {
            return "How serious is the problem the customer reports?";
        }

        /** Ordered from least to most severe, so a higher score means a more severe issue. */
        public List<String> options() {
            return List.of(
                    Severity.MINOR.name(),
                    Severity.MAJOR.name(),
                    Severity.BLOCKER.name()
            );
        }
    }
}
