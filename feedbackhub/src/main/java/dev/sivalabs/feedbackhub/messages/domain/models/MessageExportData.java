package dev.sivalabs.feedbackhub.messages.domain.models;

import java.util.List;

public record MessageExportData(AdminMessageDto message, List<AdminReplyDto> replies) {}
