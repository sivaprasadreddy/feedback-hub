package dev.sivalabs.feedbackhub.messages;

import jakarta.validation.constraints.NotBlank;

record EditReplyForm(
        @NotBlank(message = "Reply is required") String content) {}
