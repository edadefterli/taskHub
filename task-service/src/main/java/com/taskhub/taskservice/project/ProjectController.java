package com.taskhub.taskservice.project;

import com.taskhub.taskservice.project.dto.ProjectRequest;
import com.taskhub.taskservice.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "CRUD operations on projects")
class ProjectController {

    private final ProjectService projectService;

    ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "List projects", description = "Returns a paginated list of all projects.")
    @ApiResponse(responseCode = "200", description = "Page of projects")
    Page<ProjectResponse> list(Pageable pageable) {
        return projectService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project by id")
    @ApiResponse(responseCode = "200", description = "Project found")
    @ApiResponse(responseCode = "404", description = "Project not found")
    ProjectResponse get(@PathVariable UUID id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a project")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "400", description = "Invalid payload or unknown owner")
    ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project")
    @ApiResponse(responseCode = "200", description = "Project updated")
    @ApiResponse(responseCode = "400", description = "Invalid payload or unknown owner")
    @ApiResponse(responseCode = "404", description = "Project not found")
    ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a project")
    @ApiResponse(responseCode = "204", description = "Project deleted")
    @ApiResponse(responseCode = "404", description = "Project not found")
    void delete(@PathVariable UUID id) {
        projectService.delete(id);
    }
}
