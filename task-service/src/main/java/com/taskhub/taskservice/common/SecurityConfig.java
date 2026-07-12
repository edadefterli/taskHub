package com.taskhub.taskservice.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // /api/v1/** is open for Session 2 (CRUD before auth exists). Session 3
        // (JWT + RBAC) replaces this permitAll with real authentication/ownership
        // checks per SPEC.md's session ordering.
        // CSRF is disabled because this API is stateless (Bearer JWT in Session 3,
        // no session cookies), where CSRF protection doesn't apply.
        // /error must stay permitAll too: any exception thrown while handling a
        // permitAll request (e.g. a 400 from bean validation) forwards internally
        // to /error, and that forward re-enters this same filter chain — without
        // this, the real error status gets masked by a 403 from the forward itself.
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/v1/**", "/error").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
