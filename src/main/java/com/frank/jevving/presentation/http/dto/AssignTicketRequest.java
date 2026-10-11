package com.frank.jevving.presentation.http.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AssignTicketRequest", description = "The support team picked by the human reviewer")
public record AssignTicketRequest(

        @Schema(description = "Id of the support team", examples = "devops")
        @NotBlank
        String teamId) {
}
