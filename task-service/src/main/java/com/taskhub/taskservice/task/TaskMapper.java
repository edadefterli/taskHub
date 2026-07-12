package com.taskhub.taskservice.task;

import com.taskhub.taskservice.tag.Tag;
import com.taskhub.taskservice.task.dto.TaskResponse;

import java.util.Set;
import java.util.stream.Collectors;

final class TaskMapper {

    private TaskMapper() {
    }

    static TaskResponse toResponse(Task task) {
        Set<String> tagNames = task.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                tagNames,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
