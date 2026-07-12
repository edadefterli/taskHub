package com.taskhub.taskservice.task.dto;

import com.taskhub.taskservice.task.TaskStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Set<String> tagNames,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
