package com.taskhub.taskservice.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// ownerId is client-supplied until Session 3 wires JWT auth; it will then be
// derived from the authenticated principal and dropped from this request.
public record ProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull UUID ownerId) {
}
