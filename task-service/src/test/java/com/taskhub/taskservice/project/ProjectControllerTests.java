package com.taskhub.taskservice.project;

import com.taskhub.taskservice.common.ResourceNotFoundException;
import com.taskhub.taskservice.common.SecurityConfig;
import com.taskhub.taskservice.project.dto.ProjectRequest;
import com.taskhub.taskservice.project.dto.ProjectResponse;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
class ProjectControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void should_returnCreated_when_creatingValidProject() throws Exception {
        UUID ownerId = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse(
                UUID.randomUUID(), "Launch", "Launch project", ownerId,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(projectService.create(any(ProjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Launch","description":"Launch project","ownerId":"%s"}
                                """.formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Launch"));
    }

    @Test
    void should_returnBadRequest_when_creatingProjectWithBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":null,"ownerId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnNotFound_when_projectDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(projectService.get(eq(id))).thenThrow(new ResourceNotFoundException("Project not found: " + id));

        mockMvc.perform(get("/api/v1/projects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void should_returnPagedProjects_when_listing() throws Exception {
        ProjectResponse response = new ProjectResponse(
                UUID.randomUUID(), "Launch", null, UUID.randomUUID(),
                OffsetDateTime.now(), OffsetDateTime.now());
        Page<ProjectResponse> page = new PageImpl<>(List.of(response));

        when(projectService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Launch"));
    }
}
