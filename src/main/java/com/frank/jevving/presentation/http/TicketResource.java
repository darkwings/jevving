package com.frank.jevving.presentation.http;

import com.frank.jevving.application.usecases.AssignTicketUseCase;
import com.frank.jevving.application.usecases.CreateTicketUseCase;
import com.frank.jevving.presentation.http.dto.AssignTicketRequest;
import com.frank.jevving.presentation.http.dto.CreateTicketRequest;
import com.frank.jevving.presentation.http.dto.ErrorResponse;
import com.frank.jevving.presentation.http.dto.TicketResponse;
import com.frank.jevving.presentation.http.mappers.TicketHttpMapper;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

@Path("/api/tickets")
@Tag(name = "Tickets", description = "Support tickets dispatched by Jev")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TicketResource {

    private final CreateTicketUseCase createTicket;
    private final AssignTicketUseCase assignTicket;
    private final TicketHttpMapper mapper;

    public TicketResource(CreateTicketUseCase createTicket, AssignTicketUseCase assignTicket,
                          TicketHttpMapper mapper) {
        this.createTicket = createTicket;
        this.assignTicket = assignTicket;
        this.mapper = mapper;
    }

    @POST
    @Operation(summary = "Create a ticket",
            description = "Jev assesses the request for urgency and severity and picks the support team "
                    + "that should handle it; the resulting ticket is stored and returned.")
    @APIResponse(responseCode = "201", description = "Ticket created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TicketResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "502", description = "Jev returned an unusable assessment",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Uni<RestResponse<TicketResponse>> create(@NotNull @Valid CreateTicketRequest request) {
        return createTicket.create(mapper.toCommand(request))
                .map(mapper::toResponse)
                .map(ticket -> RestResponse.status(RestResponse.Status.CREATED, ticket));
    }

    @PUT
    @Path("/{id}/assignment")
    @Operation(summary = "Assign a ticket waiting for review",
            description = "A human picks the support team for a ticket in NEEDS_REVIEW. "
                    + "The ticket becomes ASSIGNED; Jev's suggestion is kept.")
    @APIResponse(responseCode = "200", description = "Ticket assigned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TicketResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "404", description = "Ticket not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Ticket is not waiting for review",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Support team not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Uni<TicketResponse> assign(@PathParam("id") UUID id, @NotNull @Valid AssignTicketRequest request) {
        return assignTicket.assign(mapper.toCommand(id, request))
                .map(mapper::toResponse);
    }
}
