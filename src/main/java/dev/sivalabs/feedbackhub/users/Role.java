package dev.sivalabs.feedbackhub.users;

public enum Role {
    ROLE_ADMIN("ADMIN"),
    ROLE_USER("USER");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
