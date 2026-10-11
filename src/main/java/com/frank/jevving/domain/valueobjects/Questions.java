package com.frank.jevving.domain.valueobjects;

public record Questions(
        Question.Urgency urgency,
        Question.Team team,
        Question.SeverityScore severityScore) {
}
