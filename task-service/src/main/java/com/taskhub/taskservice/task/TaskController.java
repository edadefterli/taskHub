package com.taskhub.taskservice.task;

import com.taskhub.taskservice.task.dto.TaskRequest;
import com.taskhub.taskservice.task.dto.TaskResponse;
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
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@Tag(name = "Tasks", description = "CRUD operations on tasks nested under a project")
class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List tasks in a project", description = "Returns a paginated list of tasks for the given project.")
    @ApiResponse(responseCode = "200", description = "Page of tasks")
    @ApiResponse(responseCode = "404", description = "Project not found")
    Page<TaskResponse> list(@PathVariable UUID projectId, Pageable pageable) {
        return taskService.list(projectId, pageable);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get a task by id")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Project or task not found")
    TaskResponse get(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        return taskService.get(projectId, taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task", description = "tagNames create-or-associate existing Tag rows by name.")
    @ApiResponse(responseCode = "201", description = "Task created")
    @ApiResponse(responseCode = "400", description = "Invalid payload")
    @ApiResponse(responseCode = "404", description = "Project not found")
    TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update a task")
    @ApiResponse(responseCode = "200", description = "Task updated")
    @ApiResponse(responseCode = "400", description = "Invalid payload")
    @ApiResponse(responseCode = "404", description = "Project or task not found")
    TaskResponse update(@PathVariable UUID projectId, @PathVariable UUID taskId, @Valid @RequestBody TaskRequest request) {
        return taskService.update(projectId, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "404", description = "Project or task not found")
    void delete(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.delete(projectId, taskId);
    }
}
