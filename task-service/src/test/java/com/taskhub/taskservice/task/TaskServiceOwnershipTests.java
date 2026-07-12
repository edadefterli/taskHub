package com.taskhub.taskservice.task;

import com.taskhub.taskservice.project.Project;
import com.taskhub.taskservice.project.ProjectRepository;
import com.taskhub.taskservice.tag.TagRepository;
import com.taskhub.taskservice.task.dto.TaskRequest;
import com.taskhub.taskservice.user.Role;
import com.taskhub.taskservice.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceOwnershipTests {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private final TaskService taskService = new TaskService(taskRepository, projectRepository, tagRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_denyCreate_when_currentUserIsNeitherProjectOwnerNorAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = existingProject(projectId, ownerId);
        authenticateAs(UUID.randomUUID(), "USER");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        TaskRequest request = new TaskRequest("Hijack task", null, TaskStatus.TODO, null, Set.of());

        assertThatThrownBy(() -> taskService.create(projectId, request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void should_denyDelete_when_currentUserIsNeitherProjectOwnerNorAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Project project = existingProject(projectId, ownerId);
        Task task = Task.builder().id(taskId).project(project).title("t").status(TaskStatus.TODO).build();
        authenticateAs(UUID.randomUUID(), "USER");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(projectId, taskId)).isInstanceOf(AccessDeniedException.class);
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
