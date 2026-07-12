package com.taskhub.taskservice.task.dto;

import com.taskhub.taskservice.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull TaskStatus status,
        @FutureOrPresent LocalDate dueDate,
        Set<@NotBlank String> tagNames) {
}
