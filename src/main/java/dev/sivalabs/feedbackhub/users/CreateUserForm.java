package dev.sivalabs.feedbackhub.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record CreateUserForm(
        @NotBlank(message = "Name is required") String name,

        @NotBlank(message = "Email is required") @Email(message = "Email address must be valid")
        String email,

        @NotNull(message = "Role is required") Role role) {}
