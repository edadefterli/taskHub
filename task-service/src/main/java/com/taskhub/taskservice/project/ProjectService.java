package com.taskhub.taskservice.project;

import com.taskhub.taskservice.project.dto.ProjectRequest;
import com.taskhub.taskservice.project.dto.ProjectResponse;
import com.taskhub.taskservice.user.User;
import com.taskhub.taskservice.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        User owner = findOwnerOrThrow(request.ownerId());

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
        User owner = findOwnerOrThrow(request.ownerId());

        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwner(owner);

        // saveAndFlush (not save): forces the insert/update now so Hibernate's
        // @CreationTimestamp/@UpdateTimestamp are populated before the response
        // is built, instead of returning null timestamps until the next read.
        return ProjectMapper.toResponse(projectRepository.saveAndFlush(project));
    }

    @Transactional
    void delete(UUID id) {
        Project project = findProjectOrThrow(id);
        projectRepository.delete(project);
    }

    private Project findProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + id));
    }

    private User findOwnerOrThrow(UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner not found: " + ownerId));
    }
}
