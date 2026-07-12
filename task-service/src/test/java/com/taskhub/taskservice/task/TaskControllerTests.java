package com.taskhub.taskservice.task;

import com.taskhub.taskservice.auth.JwtConfig;
import com.taskhub.taskservice.common.ResourceNotFoundException;
import com.taskhub.taskservice.common.SecurityConfig;
import com.taskhub.taskservice.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class TaskControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void should_returnUnauthorized_when_noTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_returnCreated_when_creatingTaskWithTags() throws Exception {
        UUID projectId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                UUID.randomUUID(), projectId, "Ship it", null, TaskStatus.TODO, null,
                Set.of("urgent"), OffsetDateTime.now(), OffsetDateTime.now());

        when(taskService.create(eq(projectId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ship it","status":"TODO","tagNames":["urgent"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Ship it"))
                .andExpect(jsonPath("$.tagNames[0]").value("urgent"));
    }

    @Test
    void should_returnNotFound_when_projectDoesNotExist() throws Exception {
        UUID projectId = UUID.randomUUID();

        when(taskService.list(eq(projectId), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Project not found: " + projectId));

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_returnPagedTasks_when_listing() throws Exception {
        UUID projectId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                UUID.randomUUID(), projectId, "Ship it", null, TaskStatus.TODO, null,
                Set.of(), OffsetDateTime.now(), OffsetDateTime.now());
        Page<TaskResponse> page = new PageImpl<>(List.of(response));

        when(taskService.list(eq(projectId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Ship it"));
    }
}
