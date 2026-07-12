package com.taskhub.taskservice.auth;

import com.taskhub.taskservice.user.Role;
import com.taskhub.taskservice.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private final SecretKeySpec secretKey = new SecretKeySpec(
            "test-secret-key-that-is-at-least-32-bytes-long!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
    private final JwtDecoder jwtDecoder =
            NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    private final JwtService jwtService = new JwtService(jwtEncoder, 3600);

    @Test
    void should_encodeUserClaims_when_generatingToken() {
        User user = User.builder().id(UUID.randomUUID()).email("a@b.com").role(Role.ADMIN).build();

        String token = jwtService.generateToken(user);
        Jwt decoded = jwtDecoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo(user.getId().toString());
        assertThat(decoded.getClaimAsString("email")).isEqualTo("a@b.com");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void should_setExpiryInFuture_when_generatingToken() {
        User user = User.builder().id(UUID.randomUUID()).email("a@b.com").role(Role.USER).build();

        String token = jwtService.generateToken(user);
        Jwt decoded = jwtDecoder.decode(token);

        assertThat(decoded.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void should_rejectExpiredToken_when_decoding() {
        Instant past = Instant.now().minusSeconds(120);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(past.minusSeconds(60))
                .expiresAt(past)
                .build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> jwtDecoder.decode(expiredToken)).isInstanceOf(JwtException.class);
    }
}
