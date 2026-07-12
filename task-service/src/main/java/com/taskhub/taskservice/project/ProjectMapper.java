package com.taskhub.taskservice.project;

import com.taskhub.taskservice.project.dto.ProjectResponse;

final class ProjectMapper {

    private ProjectMapper() {
    }

    static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
