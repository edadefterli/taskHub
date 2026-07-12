package com.taskhub.taskservice.auth;

import com.taskhub.taskservice.auth.dto.AuthResponse;
import com.taskhub.taskservice.auth.dto.LoginRequest;
import com.taskhub.taskservice.auth.dto.RegisterRequest;
import com.taskhub.taskservice.common.ConflictException;
import com.taskhub.taskservice.user.Role;
import com.taskhub.taskservice.user.User;
import com.taskhub.taskservice.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationSeconds;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        User saved = userRepository.saveAndFlush(user);

        return issueToken(saved);
    }

    @Transactional(readOnly = true)
    AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", expirationSeconds);
    }
}
