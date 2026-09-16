package dev.sivalabs.feedbackhub.users;

record ChangePasswordCmd(String currentPassword, String newPassword) {}

record AccountDetails(String name, String email, Role role, boolean hasProfilePicture) {}

record CreateUserCmd(String name, String email, Role role) {}

record ImportUsersResult(int importedCount, java.util.List<ImportUserError> errors) {
    boolean successful() {
        return errors.isEmpty();
    }
}

record ImportUserError(int rowNumber, String message) {}

record EditUserCmd(Role role, boolean active) {}

record UserFilterQuery(Role role, Boolean active) {}
