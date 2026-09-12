package dev.sivalabs.speakup.users;

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUser implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean active;

    public SecurityUser(Long id, String name, String email, String password, Role role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }
}
