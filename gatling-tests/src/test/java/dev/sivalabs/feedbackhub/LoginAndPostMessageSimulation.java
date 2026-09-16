package dev.sivalabs.feedbackhub;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.UUID;

public class LoginAndPostMessageSimulation extends Simulation {
    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:8080").replaceAll("/+$", "");
    private static final int USERS = Integer.getInteger("users", 10);
    private static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 10);

    private final FeederBuilder.FileBased<String> credentials =
            csv("data/users.csv").circular();

    private final HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL)
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptLanguageHeader("en-US,en;q=0.9");

    private final ScenarioBuilder loginAndPostMessage = scenario("User login and post message")
            .feed(credentials)
            .exec(http("Open login page")
                    .get("/login")
                    .check(status().is(200))
                    .check(css("input[name='_csrf']", "value").saveAs("loginCsrf")))
            .exec(http("Log in")
                    .post("/login")
                    .formParam("username", "#{email}")
                    .formParam("password", "#{password}")
                    .formParam("_csrf", "#{loginCsrf}")
                    .disableFollowRedirect()
                    .check(status().is(302))
                    .check(header("Location").is(BASE_URL + "/")))
            .exec(http("Open message feed")
                    .get("/")
                    .check(status().is(200))
                    .check(css("form[action='/messages'] input[name='_csrf']", "value")
                            .saveAs("messageCsrf")))
            .exec(session -> session.set(
                    "message", "Gatling feedback from " + session.getString("email") + " " + UUID.randomUUID()))
            .exec(http("Post message")
                    .post("/messages")
                    .formParam("content", "#{message}")
                    .formParam("postingIdentity", "IDENTIFIED")
                    .formParam("_csrf", "#{messageCsrf}")
                    .disableFollowRedirect()
                    .check(status().is(302))
                    .check(header("Location").is(BASE_URL + "/")))
            .exec(http("Verify posted message")
                    .get("/")
                    .check(status().is(200))
                    .check(substring("#{message}").exists()));

    {
        setUp(loginAndPostMessage.injectOpen(rampUsers(USERS).during(Duration.ofSeconds(RAMP_SECONDS))))
                .protocols(httpProtocol)
                .assertions(global().failedRequests().count().is(0L));
    }
}
