package dev.sivalabs.feedbackhub.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ChangePasswordForm(
        @NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword) {}
