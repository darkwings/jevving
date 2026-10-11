# jevving

> [!WARNING]
> **This is a proof of concept, not production-ready software.**
>
> - **Everything is in memory**: tickets are lost on every restart.
> - **Support teams are hard-coded** in `InMemorySupportTeamRepository`.
> - **There is no API to manage support teams**: to add, remove or change one you edit the code and restart.
> - No authentication: `submittedBy` is trusted as sent by the client.

Proof of concept of a **support ticket dispatcher** powered by [Jev](https://docs.typesafe.ai/introduction), TypeSafe's System One model.

When a customer opens a ticket, Jev reads the request and answers three typed questions in a single call:

| Question | Jev type | Used for |
|---|---|---|
| Does the customer's message convey a need for immediate help? | `noul` (0–1) | `urgency` (`NORMAL` / `URGENT`), `URGENT` above `urgency.limit` |
| Which team is best placed to resolve this request? | `choice` among the support teams | the team the ticket is dispatched to |
| How serious is the problem the customer reports? | `score` on `MINOR` → `MAJOR` → `BLOCKER` | `severity`, the most likely level |

A ticket is not assigned automatically, but goes to `NEEDS_REVIEW`, when Jev is not confident enough about the team (confidence below `dispatch.confidence-limit`) or when it finds the request too vague to choose one (it picks the special `unclear` option). The ticket keeps Jev's choice as a suggestion, and a human assigns it through the API.

## Requirements

- **JDK 25** (Gradle fails with `invalid source release: 25` on older JDKs; set `JAVA_HOME` accordingly).
- A **TypeSafe API key**, exported as `TYPESAFE_API_KEY`. The app does not start without it. Tests don't need it.

The Gradle wrapper is included, nothing else to install.

## Running

```shell
export TYPESAFE_API_KEY=<your key>
./gradlew quarkusDev
```

Instead of exporting the variable, you can put `TYPESAFE_API_KEY=<your key>` in a `.env` file in the project root (already git-ignored).

The app listens on <http://localhost:8080>. In dev mode you also get:

- Swagger UI: <http://localhost:8080/q/swagger-ui>
- OpenAPI spec: <http://localhost:8080/q/openapi>
- Quarkus Dev UI: <http://localhost:8080/q/dev/>

### Packaged

```shell
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

`TYPESAFE_API_KEY` must be set here too. Swagger UI is only available in dev mode; the spec at `/q/openapi` is always exposed.

## Try it out

A guided tour with `curl`. [`jq`](https://jqlang.org/) is optional: it pretty-prints the responses and lets you capture the ticket id in a variable. If you prefer clicking, every call can also be made from the Swagger UI at <http://localhost:8080/q/swagger-ui>.

> Answers come from Jev, so the team, severity and urgency you get may differ from the ones shown here. That is the point of the PoC: see how Jev classifies real requests.

### 1. Start the app

```shell
export TYPESAFE_API_KEY=<your key>
./gradlew quarkusDev
```

In another terminal, check it is up:

```shell
curl http://localhost:8080/api/health
```

### 2. Create tickets that Jev should dispatch on its own

An infrastructure outage, expected to go to `devops`, urgent and `BLOCKER`:

```shell
curl -s -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "Our production site has been down for 20 minutes, the AWS load balancer returns 503 to every customer. Please help ASAP!", "submittedBy": "alice"}' | jq
```

A software bug, expected to go to `developers`, not urgent:

```shell
curl -s -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "When I export the monthly report to CSV the dates are shown in the wrong format. Not blocking, just annoying.", "submittedBy": "bob"}' | jq
```

A commercial issue, expected to go to `business`:

```shell
curl -s -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "We are your biggest customer and our contract renewal terms were changed without notice. I want to talk to someone today.", "submittedBy": "carol"}' | jq
```

A marketing request, expected to go to `sales`:

```shell
curl -s -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "Can you send us the brochure and the logo kit for the trade fair next month?", "submittedBy": "dave"}' | jq
```

Each response has `dispatchStatus: "ASSIGNED"` when Jev's confidence on the team is at least `dispatch.confidence-limit` (0.7 by default).

### 3. Send a ticket to human review

A vague request should make Jev answer `unclear` (a special option added to the teams it can choose from), which always sends the ticket to review, whatever its confidence:

```shell
curl -s -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "Something is not working as expected, can someone look into it?", "submittedBy": "erin"}' | jq
```

Jev can be very confident even on vague requests, so the other way to force a review is a higher threshold. In a first test a ticket like this needed `0.99` to go to review (the default is `0.7`):

```shell
DISPATCH_CONFIDENCE_LIMIT=0.99 ./gradlew quarkusDev
```

A ticket in review looks like this: no assigned team, and a suggested team (Jev's choice, or its most probable real team when it answered `unclear`).

```json
{
  "dispatchStatus": "NEEDS_REVIEW",
  "assignedTeam": null,
  "suggestedTeam": { "id": "developers", "capability": "Support on software bugs" }
}
```

### 4. Assign it manually

Create a ticket and keep its id:

```shell
ID=$(curl -s -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "Something is not working as expected, can someone look into it?", "submittedBy": "erin"}' | jq -r .id)
echo "$ID"
```

If the ticket is in `NEEDS_REVIEW`, assign it to a team (any id from [Support teams](#support-teams)):

```shell
curl -s -X PUT "http://localhost:8080/api/tickets/$ID/assignment" \
  -H 'Content-Type: application/json' \
  -d '{"teamId": "developers"}' | jq
```

The ticket becomes `ASSIGNED` to `developers`; `suggestedTeam` still shows what Jev proposed.

### 5. See the errors

Assigning the same ticket again is a conflict, since it is no longer in review (`409`):

```shell
curl -s -w '\nHTTP %{http_code}\n' -X PUT "http://localhost:8080/api/tickets/$ID/assignment" \
  -H 'Content-Type: application/json' \
  -d '{"teamId": "business"}'
```

An unknown team (`422`) and an unknown ticket (`404`):

```shell
curl -s -w '\nHTTP %{http_code}\n' -X PUT "http://localhost:8080/api/tickets/$ID/assignment" \
  -H 'Content-Type: application/json' \
  -d '{"teamId": "marketing"}'

curl -s -w '\nHTTP %{http_code}\n' -X PUT http://localhost:8080/api/tickets/00000000-0000-0000-0000-000000000000/assignment \
  -H 'Content-Type: application/json' \
  -d '{"teamId": "devops"}'
```

A blank request (`400`):

```shell
curl -s -w '\nHTTP %{http_code}\n' -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": " ", "submittedBy": "alice"}'
```

Remember that everything is in memory: after a restart the ids you saved no longer exist.

## Configuration

All in `src/main/resources/application.properties`. Every property can be overridden with an environment variable (e.g. `DISPATCH_CONFIDENCE_LIMIT=0.8`) or a system property (`-Ddispatch.confidence-limit=0.8`).

| Property | Default | Meaning |
|---|---|---|
| `jev.api-key` | `${TYPESAFE_API_KEY}` | TypeSafe API key |
| `jev.base-url` | `https://api.typesafe.ai` | Jev API base URL |
| `jev.model` | `jev-latest` | Jev model |
| `urgency.limit` | `0.9` | A ticket is urgent when Jev's urgency probability is above this value |
| `dispatch.confidence-limit` | `0.7` | Below this confidence on the team choice, the ticket needs a human review |

### Support teams

The teams Jev can choose from are **hard-coded** in `InMemorySupportTeamRepository`, and **there is no API to manage them**: to change them, edit that class and restart the app.

| Id | Capability (sent to Jev as the option description) |
|---|---|
| `devops` | AWS Dev-ops support for infrastructure problems |
| `developers` | Support on software bugs |
| `business` | Business problems, big customer relationship |
| `sales` | Marketing support |

The capability is what Jev uses to pick a team, so write it as a clear description of what the team handles. Ids must be unique, and they are the values accepted by the assignment endpoint.

Besides these, every question to Jev includes a special `unclear` option ("The request is too vague or ambiguous to tell which team should handle it"). It is not a team: it lives in the domain, not in the repository, so it cannot be assigned and the id `unclear` is reserved. When Jev picks it, the ticket goes to review whatever its confidence.

## API

### Create a ticket

`POST /api/tickets`

```shell
curl -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"request": "Production is down, customers cannot log in!", "submittedBy": "alice"}'
```

`submittedBy` is the id of the user opening the ticket, taken as is.

`201 Created`:

```json
{
  "id": "6f1c2a8e-3b7d-4e0a-9c55-2d8f1e4b7a10",
  "request": "Production is down, customers cannot log in!",
  "submittedBy": "alice",
  "urgency": "URGENT",
  "severity": "BLOCKER",
  "dispatchStatus": "ASSIGNED",
  "assignedTeam": { "id": "devops", "capability": "AWS Dev-ops support for infrastructure problems" },
  "suggestedTeam": null
}
```

When Jev is confident, `suggestedTeam` is `null`. When it is not, `dispatchStatus` is `NEEDS_REVIEW`, `assignedTeam` is `null` and `suggestedTeam` holds Jev's choice.

| Status | When |
|---|---|
| `201` | Ticket created (assigned or waiting for review) |
| `400` | `request` or `submittedBy` missing or blank |
| `502` | Jev chose a team that does not exist |

### Assign a ticket waiting for review

`PUT /api/tickets/{id}/assignment`

```shell
curl -X PUT http://localhost:8080/api/tickets/6f1c2a8e-3b7d-4e0a-9c55-2d8f1e4b7a10/assignment \
  -H 'Content-Type: application/json' \
  -d '{"teamId": "business"}'
```

`200 OK` with the ticket, now `ASSIGNED` to the chosen team. Jev's suggestion is kept in `suggestedTeam`.

| Status | When |
|---|---|
| `200` | Ticket assigned |
| `400` | `teamId` missing or blank |
| `404` | No ticket with that id |
| `409` | The ticket is not in `NEEDS_REVIEW` |
| `422` | No support team with that id |

`404`, `409`, `422` and `502` return a body like `{"message": "Ticket is not waiting for review: …"}`.

## Tests

```shell
./gradlew test
./gradlew test --tests '*TicketResourceTest'
```

Jev is stubbed with WireMock, so tests run offline and without an API key.

## Architecture

Hexagonal, under `com.frank.jevving`:

- `domain`: entities (`Ticket`, `SupportTeam`), value objects, domain services (`UrgencyDecider`, `TeamDecider`), exceptions and ports (`TicketRepository`, `SupportTeamRepository`, `RequestAssessor`). It knows nothing about Jev or HTTP.
- `application`: use cases `CreateTicketUseCase` and `AssignTicketUseCase` and their services, which orchestrate the ports.
- `infrastructure`: adapters. `jev/JevAssessor` implements `RequestAssessor` through the Jev REST client; `persistence/repositories` holds the in-memory repositories.
- `presentation/http`: REST resource, DTOs, DTO ↔ command/domain mappers and the mapping of domain exceptions to HTTP status codes.

Everything is reactive (Mutiny `Uni`), from the REST endpoints down to the Jev client.

Stack: Quarkus 3.40, Java 25, Gradle, Lombok, Hibernate Validator, SmallRye OpenAPI; tests with JUnit 5, AssertJ, REST Assured and WireMock.
