package dev.sivalabs.feedbackhub.users;

record ChangePasswordCmd(String currentPassword, String newPassword) {}
