package dev.sivalabs.feedbackhub.users.domain.models;

public record ImportUsersResult(int importedCount, java.util.List<ImportUserError> errors) {
    public boolean successful() {
        return errors.isEmpty();
    }
}
