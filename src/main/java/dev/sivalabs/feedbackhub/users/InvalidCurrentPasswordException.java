package dev.sivalabs.feedbackhub.users;

class InvalidCurrentPasswordException extends RuntimeException {
    InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
