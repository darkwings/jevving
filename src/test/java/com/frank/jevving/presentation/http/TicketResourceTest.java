package com.frank.jevving.presentation.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.frank.jevving.infrastructure.jev.WireMockJevResource;
import com.frank.jevving.presentation.http.dto.TicketResponse;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(WireMockJevResource.class)
class TicketResourceTest {

    @BeforeEach
    void resetJev() {
        WireMockJevResource.server().resetAll();
    }

    private static void stubJev(double teamConfidence) {
        stubJev("devops", teamConfidence);
    }

    private static void stubJev(String teamId, double teamConfidence) {
        WireMockJevResource.server().stubFor(post(urlEqualTo("/v1/systemone")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "model": "jev-1.13.0",
                          "answers": {
                            "urgent": {"type": "noul", "noul": 0.97},
                            "team": {
                              "type": "choice", "choice": "%s", "confidence": %s,
                              "probabilities": {"devops": 0.9, "developers": 0.05, "business": 0.03, "sales": 0.02}
                            },
                            "severity": {
                              "type": "score", "score": 1.8, "confidence": 0.8,
                              "legend": {"0": "MINOR", "1": "MAJOR", "2": "BLOCKER"},
                              "probabilities": {"0": 0.02, "1": 0.16, "2": 0.82}
                            }
                          },
                          "usage": {"input_tokens": 120, "output_tokens": 3}
                        }
                        """.formatted(teamId, teamConfidence))));
    }

    private static TicketResponse createTicket(String request) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("request", request, "submittedBy", "alice"))
                .when().post("/api/tickets")
                .then().statusCode(201)
                .extract().as(TicketResponse.class);
    }

    @Test
    void createsAssignedTicket() {
        stubJev(0.9);

        TicketResponse ticket = createTicket("Production is down, customers can't log in!");

        assertThat(ticket.id()).isNotNull();
        assertThat(ticket.request()).isEqualTo("Production is down, customers can't log in!");
        assertThat(ticket.submittedBy()).isEqualTo("alice");
        assertThat(ticket.urgency()).isEqualTo("URGENT");
        assertThat(ticket.severity()).isEqualTo("BLOCKER");
        assertThat(ticket.dispatchStatus()).isEqualTo("ASSIGNED");
        assertThat(ticket.assignedTeam().id()).isEqualTo("devops");
        assertThat(ticket.suggestedTeam()).isNull();
    }

    @Test
    void createsTicketThatNeedsReviewWhenJevIsNotConfident() {
        stubJev(0.4);

        TicketResponse ticket = createTicket("Something is off with my account");

        assertThat(ticket.dispatchStatus()).isEqualTo("NEEDS_REVIEW");
        assertThat(ticket.assignedTeam()).isNull();
        assertThat(ticket.suggestedTeam().id()).isEqualTo("devops");
    }

    @Test
    void createsTicketThatNeedsReviewWhenJevFindsTheRequestUnclear() {
        stubJev("unclear", 0.99);

        TicketResponse ticket = createTicket("Something is not working as expected");

        assertThat(ticket.dispatchStatus()).isEqualTo("NEEDS_REVIEW");
        assertThat(ticket.assignedTeam()).isNull();
        assertThat(ticket.suggestedTeam().id()).isEqualTo("devops");
    }

    @Test
    void unclearIsNotAnAssignableTeam() {
        stubJev("unclear", 0.99);
        TicketResponse created = createTicket("Something is not working as expected");

        assignTicket(created.id(), "unclear")
                .then().statusCode(422);
    }

    @Test
    void unknownTeamFromJevIsABadGateway() {
        stubJev("marketing", 0.9);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("request", "Production is down!", "submittedBy", "alice"))
                .when().post("/api/tickets")
                .then().statusCode(502)
                .body("message", containsString("marketing"));
    }

    @Test
    void assignsTicketWaitingForReview() {
        stubJev(0.4);
        TicketResponse created = createTicket("Something is off with my account");

        TicketResponse assigned = assignTicket(created.id(), "business")
                .then().statusCode(200)
                .extract().as(TicketResponse.class);

        assertThat(assigned.id()).isEqualTo(created.id());
        assertThat(assigned.dispatchStatus()).isEqualTo("ASSIGNED");
        assertThat(assigned.assignedTeam().id()).isEqualTo("business");
        assertThat(assigned.suggestedTeam().id()).isEqualTo("devops");
    }

    @Test
    void assigningAnAssignedTicketIsAConflict() {
        stubJev(0.9);
        TicketResponse created = createTicket("Production is down!");

        assignTicket(created.id(), "business")
                .then().statusCode(409)
                .body("message", containsString(created.id().toString()));
    }

    @Test
    void assigningAMissingTicketIsNotFound() {
        assignTicket(UUID.randomUUID(), "business")
                .then().statusCode(404);
    }

    @Test
    void assigningAnUnknownTeamIsUnprocessable() {
        stubJev(0.4);
        TicketResponse created = createTicket("Something is off with my account");

        assignTicket(created.id(), "marketing")
                .then().statusCode(422)
                .body("message", containsString("marketing"));
    }

    @Test
    void assigningRequiresATeam() {
        assignTicket(UUID.randomUUID(), " ")
                .then().statusCode(400);
    }

    private static Response assignTicket(UUID id, String teamId) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("teamId", teamId))
                .when().put("/api/tickets/{id}/assignment", id);
    }

    @Test
    void rejectsBlankFields() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"request": " ", "submittedBy": "alice"}
                        """)
                .when().post("/api/tickets")
                .then().statusCode(400);
    }

    @Test
    void documentsTheEndpointInOpenApi() {
        String spec = given().queryParam("format", "json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();

        assertThat(spec).contains("/api/tickets", "/api/tickets/{id}/assignment", "CreateTicketRequest",
                "AssignTicketRequest", "\"Ticket\"", "\"Error\"", "\"201\"", "\"409\"", "\"422\"", "\"502\"");
    }
}
