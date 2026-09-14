package dev.sivalabs.feedbackhub.users;

record ChangePasswordCmd(String currentPassword, String newPassword) {}

record AccountDetails(String name, String email, Role role, boolean hasProfilePicture) {}

record CreateUserCmd(String name, String email, Role role) {}

record EditUserCmd(Role role, boolean active) {}

record UserFilterQuery(Role role, Boolean active) {}
