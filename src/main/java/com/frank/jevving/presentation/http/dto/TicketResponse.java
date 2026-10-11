package com.frank.jevving.presentation.http.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "Ticket", description = "A support ticket, assessed and dispatched by Jev")
public record TicketResponse(

        @Schema(description = "Ticket id")
        UUID id,

        @Schema(description = "The text written by the customer")
        String request,

        @Schema(description = "Id of the user who opened the ticket")
        String submittedBy,

        @Schema(description = "Whether the customer needs immediate help", enumeration = {"NORMAL", "URGENT"})
        String urgency,

        @Schema(description = "Most likely severity", enumeration = {"MINOR", "MAJOR", "BLOCKER"})
        String severity,

        @Schema(description = "ASSIGNED when Jev was confident enough to pick a team, "
                + "NEEDS_REVIEW when a human has to pick it", enumeration = {"ASSIGNED", "NEEDS_REVIEW"})
        String dispatchStatus,

        @Schema(description = "The team the ticket is dispatched to; null while the ticket needs review",
                nullable = true)
        SupportTeamResponse assignedTeam,

        @Schema(description = "The team Jev suggests; set only for tickets that went through a human review, "
                + "as a hint for the reviewer", nullable = true)
        SupportTeamResponse suggestedTeam) {
}
