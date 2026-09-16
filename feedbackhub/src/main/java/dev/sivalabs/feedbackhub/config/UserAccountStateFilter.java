package dev.sivalabs.feedbackhub.config;

import dev.sivalabs.feedbackhub.users.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

class UserAccountStateFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;

    UserAccountStateFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser currentUser) {
            try {
                var freshUser = userDetailsService.loadUserByUsername(currentUser.getUsername());
                if (!freshUser.isEnabled()) {
                    invalidateAuthentication(request);
                } else {
                    var refreshed =
                            new UsernamePasswordAuthenticationToken(freshUser, null, freshUser.getAuthorities());
                    refreshed.setDetails(authentication.getDetails());
                    SecurityContextHolder.getContext().setAuthentication(refreshed);
                }
            } catch (UsernameNotFoundException ex) {
                invalidateAuthentication(request);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void invalidateAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
