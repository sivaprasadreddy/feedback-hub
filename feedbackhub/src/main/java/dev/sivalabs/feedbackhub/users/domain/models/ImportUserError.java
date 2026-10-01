package dev.sivalabs.feedbackhub.users.domain.models;

public record ImportUserError(int rowNumber, String message) {}
