package com.taskhub.taskservice.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
