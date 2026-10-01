package dev.sivalabs.feedbackhub.users.domain.models;

public record ChangePasswordCmd(String currentPassword, String newPassword) {}
