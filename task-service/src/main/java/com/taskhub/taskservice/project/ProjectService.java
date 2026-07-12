package com.taskhub.taskservice.project;

import com.taskhub.taskservice.common.ResourceNotFoundException;
import com.taskhub.taskservice.common.SecurityUtils;
import com.taskhub.taskservice.project.dto.ProjectRequest;
import com.taskhub.taskservice.project.dto.ProjectResponse;
import com.taskhub.taskservice.user.User;
import com.taskhub.taskservice.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    Page<ProjectResponse> list(Pageable pageable) {
        return projectRepository.findAll(pageable).map(ProjectMapper::toResponse);
    }

    @Transactional(readOnly = true)
    ProjectResponse get(UUID id) {
        return ProjectMapper.toResponse(findProjectOrThrow(id));
    }

    @Transactional
    ProjectResponse create(ProjectRequest request) {
        User owner = currentUser();

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .build();

        // saveAndFlush (not save): forces the insert/update now so Hibernate's
        // @CreationTimestamp/@UpdateTimestamp are populated before the response
        // is built, instead of returning null timestamps until the next read.
        return ProjectMapper.toResponse(projectRepository.saveAndFlush(project));
    }

    @Transactional
    ProjectResponse update(UUID id, ProjectRequest request) {
        Project project = findProjectOrThrow(id);
        requireOwnerOrAdmin(project);

        project.setName(request.name());
        project.setDescription(request.description());

        return ProjectMapper.toResponse(projectRepository.saveAndFlush(project));
    }

    @Transactional
    void delete(UUID id) {
        Project project = findProjectOrThrow(id);
        requireOwnerOrAdmin(project);
        projectRepository.delete(project);
    }

    private Project findProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void requireOwnerOrAdmin(Project project) {
        if (!SecurityUtils.isAdmin() && !project.getOwner().getId().equals(SecurityUtils.currentUserId())) {
            throw new AccessDeniedException("Not allowed to modify this project");
        }
    }
}
