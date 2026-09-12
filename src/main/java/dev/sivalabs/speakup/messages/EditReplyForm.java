package dev.sivalabs.speakup.messages;

import jakarta.validation.constraints.NotBlank;

record EditReplyForm(
        @NotBlank(message = "Reply is required") String content) {}
