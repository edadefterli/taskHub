package com.taskhub.taskservice.tag.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        OffsetDateTime createdAt) {
}
