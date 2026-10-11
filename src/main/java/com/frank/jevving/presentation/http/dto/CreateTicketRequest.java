package com.frank.jevving.presentation.http.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateTicketRequest", description = "A new support request")
public record CreateTicketRequest(

        @Schema(description = "The text written by the customer",
                examples = "Production is down, customers can't log in!")
        @NotBlank
        String request,

        @Schema(description = "Id of the user opening the ticket", examples = "alice")
        @NotBlank
        String submittedBy) {
}
