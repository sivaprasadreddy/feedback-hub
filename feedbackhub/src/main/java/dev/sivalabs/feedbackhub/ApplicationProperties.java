package dev.sivalabs.feedbackhub;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record ApplicationProperties(
        @NotNull @Positive @DefaultValue("10") Integer feedPageSize,
        @NotNull @Positive @DefaultValue("20") Integer adminPageSize) {}
