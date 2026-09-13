package dev.sivalabs.feedbackhub.messages;

import jakarta.validation.constraints.NotBlank;

record EditMessageForm(
        @NotBlank(message = "Message is required") String content) {}
