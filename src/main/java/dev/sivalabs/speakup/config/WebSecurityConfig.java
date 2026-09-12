package dev.sivalabs.speakup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
class WebSecurityConfig {
    private static final String[] PUBLIC_RESOURCES = {
        "/css/**",
        "/js/**",
        "/images/**",
        "/webjars/**",
        "/favicon.ico",
        "/actuator/health/**",
        "/actuator/info/**",
        "/error",
        "/login"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) {
        http.authorizeHttpRequests(r -> r.requestMatchers(PUBLIC_RESOURCES)
                .permitAll()
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated());

        http.addFilterBefore(new UserAccountStateFilter(userDetailsService), AuthorizationFilter.class);

        http.formLogin(formLogin -> formLogin
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll());

        http.logout(logout -> logout.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern("/logout"))
                .logoutSuccessUrl("/login?logout")
                .permitAll());

        return http.build();
    }
}
