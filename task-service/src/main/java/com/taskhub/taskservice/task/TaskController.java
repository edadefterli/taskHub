package com.taskhub.taskservice.task;

import com.taskhub.taskservice.task.dto.TaskRequest;
import com.taskhub.taskservice.task.dto.TaskResponse;
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
class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    Page<TaskResponse> list(@PathVariable UUID projectId, Pageable pageable) {
        return taskService.list(projectId, pageable);
    }

    @GetMapping("/{taskId}")
    TaskResponse get(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        return taskService.get(projectId, taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }

    @PutMapping("/{taskId}")
    TaskResponse update(@PathVariable UUID projectId, @PathVariable UUID taskId, @Valid @RequestBody TaskRequest request) {
        return taskService.update(projectId, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.delete(projectId, taskId);
    }
}
