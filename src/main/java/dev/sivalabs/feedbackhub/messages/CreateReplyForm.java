package dev.sivalabs.feedbackhub.messages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record CreateReplyForm(
        @NotBlank(message = "Reply is required") String content,
        @NotNull(message = "Choose how to reply") PostingIdentity postingIdentity) {}
