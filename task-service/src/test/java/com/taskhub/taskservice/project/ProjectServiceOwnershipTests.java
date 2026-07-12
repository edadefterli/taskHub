package com.taskhub.taskservice.project;

import com.taskhub.taskservice.project.dto.ProjectRequest;
import com.taskhub.taskservice.user.Role;
import com.taskhub.taskservice.user.User;
import com.taskhub.taskservice.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceOwnershipTests {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectService projectService = new ProjectService(projectRepository, userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_allowUpdate_when_currentUserIsOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = existingProject(projectId, ownerId);
        authenticateAs(ownerId, "USER");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        projectService.update(projectId, new ProjectRequest("Renamed", null));

        assertThat(project.getName()).isEqualTo("Renamed");
    }

    @Test
    void should_allowUpdate_when_currentUserIsAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = existingProject(projectId, ownerId);
        authenticateAs(UUID.randomUUID(), "ADMIN");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        projectService.update(projectId, new ProjectRequest("Renamed by admin", null));

        assertThat(project.getName()).isEqualTo("Renamed by admin");
    }

    @Test
    void should_denyUpdate_when_currentUserIsNeitherOwnerNorAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = existingProject(projectId, ownerId);
        authenticateAs(UUID.randomUUID(), "USER");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.update(projectId, new ProjectRequest("Hijack", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void should_denyDelete_when_currentUserIsNeitherOwnerNorAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = existingProject(projectId, ownerId);
        authenticateAs(UUID.randomUUID(), "USER");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.delete(projectId)).isInstanceOf(AccessDeniedException.class);
    }

    private Project existingProject(UUID projectId, UUID ownerId) {
        User owner = User.builder().id(ownerId).email("owner@taskhub.dev").role(Role.USER).build();
        return Project.builder().id(projectId).name("Original").owner(owner).build();
    }

    private void authenticateAs(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(jwt, null, new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
