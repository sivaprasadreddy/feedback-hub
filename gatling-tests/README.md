# Gatling Performance Tests

Java Gatling simulations that exercise the feedbackhub user journey.

## Prerequisites

1. Start feedbackhub locally (from the repository root):

```bash
./mvnw -pl feedbackhub spring-boot:run -Dspring-boot.run.profiles=local
```

2. Wait until the app is listening on `http://localhost:8080`.

## Run simulations

Run all simulations in the module:

```bash
./mvnw -pl gatling-tests gatling:test
```

Run a specific simulation:

```bash
./mvnw -pl gatling-tests gatling:test \
  -Dgatling.simulationClass=dev.sivalabs.feedbackhub.LoginAndPostMessageSimulation
```

### Tunable system properties

| Property          | Default                 | Meaning                                                  |
|-------------------|-------------------------|----------------------------------------------------------|
| `baseUrl`         | `http://localhost:8080` | Target application URL                                   |
| `users`           | `10`                    | Users who log in and post one message                    |
| `rampSeconds`     | `10`                    | Time over which users start                              |

Example:

```bash
./mvnw -pl gatling-tests gatling:test \
  -Dgatling.simulationClass=dev.sivalabs.feedbackhub.LoginAndPostMessageSimulation \
  -DbaseUrl=http://localhost:8080 \
  -Dusers=25 \
  -DrampSeconds=30
```

HTML reports are written under `gatling-tests/target/gatling/`.

## Feeders

`src/test/resources/data/users.csv` contains active non-admin credentials. The feeder is circular, so the simulation can run more virtual users than CSV rows.
