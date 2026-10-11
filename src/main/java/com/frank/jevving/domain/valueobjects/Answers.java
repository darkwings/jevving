package com.frank.jevving.domain.valueobjects;

public record Answers(
        Answer.Urgency urgency,
        Answer.Team team,
        Answer.SeverityScore severityScore) {
}
