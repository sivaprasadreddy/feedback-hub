package dev.sivalabs.speakup.users;

class DuplicateEmailException extends RuntimeException {
    DuplicateEmailException() {
        super("Email address is already in use");
    }
}
