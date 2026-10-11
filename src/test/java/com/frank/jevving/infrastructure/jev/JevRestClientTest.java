package com.frank.jevving.infrastructure.jev;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(WireMockJevResource.class)
class JevRestClientTest {

    private static final String PATH = "/v1/systemone";

    @RestClient
    JevRestClient client;

    WireMockServer wiremock;

    @BeforeEach
    void resetWireMock() {
        wiremock = WireMockJevResource.server();
        wiremock.resetAll();
    }

    @Test
    void sendsAuthorizedRequestAndParsesAllAnswerTypes() {
        wiremock.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "model": "jev-1.13.0",
                          "answers": {
                            "urgent": {"type": "noul", "noul": 0.9},
                            "category": {
                              "type": "choice", "choice": "bug", "confidence": 0.8,
                              "probabilities": {"bug": 0.8, "feature": 0.2}
                            },
                            "severity": {
                              "type": "score", "score": 1.4, "confidence": 0.7,
                              "legend": {"0": "low", "1": "high"},
                              "probabilities": {"0": 0.6, "1": 0.4}
                            }
                          },
                          "usage": {"input_tokens": 12, "output_tokens": 3}
                        }
                        """)));

        Map<String, JevQuestion> questions = new LinkedHashMap<>();
        questions.put("urgent", new JevQuestion.Noul("Is it urgent?"));
        questions.put("category", new JevQuestion.Choice("Categorize",
                Map.of("bug", "A defect", "feature", "A request")));
        questions.put("severity", new JevQuestion.Score("Rate severity", List.of("low", "high")));

        JevResponse response = client.evaluate(new JevRequest("App crashes", "jev-latest", questions))
                .await().indefinitely();

        assertThat(response.model()).isEqualTo("jev-1.13.0");
        assertThat(response.usage()).isEqualTo(new JevResponse.Usage(12, 3));
        assertThat(response.answers().get("urgent"))
                .isInstanceOfSatisfying(JevAnswer.Noul.class, a -> assertThat(a.noul()).isEqualTo(0.9));
        assertThat(response.answers().get("category"))
                .isInstanceOfSatisfying(JevAnswer.Choice.class, a -> {
                    assertThat(a.choice()).isEqualTo("bug");
                    assertThat(a.confidence()).isEqualTo(0.8);
                    assertThat(a.probabilities()).containsEntry("bug", 0.8).containsEntry("feature", 0.2);
                });
        assertThat(response.answers().get("severity"))
                .isInstanceOfSatisfying(JevAnswer.Score.class, a -> {
                    assertThat(a.score()).isEqualTo(1.4);
                    assertThat(a.legend()).containsEntry("0", "low").containsEntry("1", "high");
                });

        wiremock.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withRequestBody(equalToJson("""
                        {
                          "state": "App crashes",
                          "model": "jev-latest",
                          "questions": {
                            "urgent": {"type": "noul", "instructions": "Is it urgent?"},
                            "category": {
                              "type": "choice", "instructions": "Categorize",
                              "criteria": {"bug": "A defect", "feature": "A request"}
                            },
                            "severity": {
                              "type": "score", "instructions": "Rate severity",
                              "criteria": ["low", "high"]
                            }
                          }
                        }
                        """)));
    }

    @Test
    void unknownAnswerTypeIsDeserializedAsNull() {
        wiremock.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "model": "jev-9.0.0",
                          "answers": {"q": {"type": "ranking", "order": ["a", "b"]}},
                          "usage": {"input_tokens": 1, "output_tokens": 1}
                        }
                        """)));

        JevResponse response = client.evaluate(new JevRequest("s", "jev-latest",
                Map.of("q", new JevQuestion.Noul("?")))).await().indefinitely();

        assertThat(response.answers()).containsKey("q");
        assertThat(response.answers().get("q")).isNull();
    }

    @Test
    void unauthorizedRaisesWebApplicationException() {
        wiremock.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"invalid api key\"}")));

        assertThatThrownBy(() -> client.evaluate(new JevRequest("s", "jev-latest",
                Map.of("q", new JevQuestion.Noul("?")))).await().indefinitely())
                .isInstanceOfSatisfying(WebApplicationException.class,
                        ex -> assertThat(ex.getResponse().getStatus()).isEqualTo(401));
    }
}
