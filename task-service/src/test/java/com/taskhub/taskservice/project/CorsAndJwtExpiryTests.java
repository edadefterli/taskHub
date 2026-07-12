package com.taskhub.taskservice.project;

import com.taskhub.taskservice.auth.JwtConfig;
import com.taskhub.taskservice.common.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real cross-cutting SecurityConfig behavior (CORS, JWT expiry), exercised against
// ProjectController as a stand-in for "any protected endpoint" - not Project-specific.
@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class CorsAndJwtExpiryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void should_allowConfiguredOrigin_when_corsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/projects")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void should_rejectDisallowedOrigin_when_corsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/projects")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_returnUnauthorized_when_tokenIsExpired() throws Exception {
        // Deliberately not using SecurityMockMvcRequestPostProcessors.jwt() here - it
        // injects an Authentication directly and never exercises the real JwtDecoder,
        // so it can't prove expiry is actually enforced. This builds a real compact
        // JWT (signed with the same secret via the real JwtEncoder bean) and sends it
        // as a genuine Authorization header, so the real decoder's exp check runs.
        Instant past = Instant.now().minusSeconds(120);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuedAt(past.minusSeconds(60))
                .expiresAt(past)
                .build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }
}
