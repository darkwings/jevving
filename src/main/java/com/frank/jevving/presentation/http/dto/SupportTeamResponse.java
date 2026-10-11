package com.frank.jevving.presentation.http.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SupportTeam", description = "The team a ticket is dispatched to")
public record SupportTeamResponse(

        @Schema(description = "Team id", examples = "devops")
        String id,

        @Schema(description = "What the team handles", examples = "AWS Dev-ops support for infrastructure problems")
        String capability) {
}
