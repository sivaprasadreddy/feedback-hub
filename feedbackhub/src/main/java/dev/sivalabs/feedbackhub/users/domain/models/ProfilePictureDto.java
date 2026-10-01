package dev.sivalabs.feedbackhub.users.domain.models;

import java.io.Serializable;

public record ProfilePictureDto(Long userId, String contentType, byte[] content) implements Serializable {}
