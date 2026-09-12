package dev.sivalabs.speakup;

import static org.testcontainers.utility.DockerImageName.parse;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@Testcontainers
public class TestcontainersConfig {
    @Container
    MailpitContainer mailpit = new MailpitContainer("axllent/mailpit:v1.31");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(parse("postgres:18-alpine"));
    }

    @Bean
    @ServiceConnection
    MailpitContainer mailpit() {
        return mailpit;
    }
}
