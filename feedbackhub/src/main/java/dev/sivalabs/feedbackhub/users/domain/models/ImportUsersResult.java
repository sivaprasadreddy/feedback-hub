package dev.sivalabs.feedbackhub.users.domain.models;

import java.util.List;

public record ImportUsersResult(int importedCount, List<ImportUserError> errors) {
    public boolean successful() {
        return errors.isEmpty();
    }
}
