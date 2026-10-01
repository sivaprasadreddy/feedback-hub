package dev.sivalabs.feedbackhub.users.domain.exceptions;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("Email address is already in use");
    }
}
