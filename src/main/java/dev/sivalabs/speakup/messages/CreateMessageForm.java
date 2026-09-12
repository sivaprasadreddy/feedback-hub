package dev.sivalabs.speakup.messages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record CreateMessageForm(
        @NotBlank(message = "Message is required") String content,
        @NotNull(message = "Choose how to post") PostingIdentity postingIdentity) {}
