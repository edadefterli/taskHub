package com.taskhub.taskservice.auth;

import com.taskhub.taskservice.auth.dto.AuthResponse;
import com.taskhub.taskservice.common.ConflictException;
import com.taskhub.taskservice.common.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void should_returnCreated_when_registeringNewUser() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("token-value", "Bearer", 3600));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@taskhub.dev","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void should_returnConflict_when_registeringDuplicateEmail() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@taskhub.dev","password":"password123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void should_returnBadRequest_when_registeringWithShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@taskhub.dev","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnToken_when_loginSucceeds() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("token-value", "Bearer", 3600));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@taskhub.dev","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"));
    }

    @Test
    void should_returnUnauthorized_when_loginFailsWithBadCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@taskhub.dev","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
