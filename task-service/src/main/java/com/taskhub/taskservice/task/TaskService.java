package com.taskhub.taskservice.task;

import com.taskhub.taskservice.common.ResourceNotFoundException;
import com.taskhub.taskservice.project.Project;
import com.taskhub.taskservice.project.ProjectRepository;
import com.taskhub.taskservice.tag.Tag;
import com.taskhub.taskservice.tag.TagRepository;
import com.taskhub.taskservice.task.dto.TaskRequest;
import com.taskhub.taskservice.task.dto.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;

    TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, TagRepository tagRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    Page<TaskResponse> list(UUID projectId, Pageable pageable) {
        requireProject(projectId);
        return taskRepository.findByProjectId(projectId, pageable).map(TaskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    TaskResponse get(UUID projectId, UUID taskId) {
        return TaskMapper.toResponse(findTaskInProjectOrThrow(projectId, taskId));
    }

    @Transactional
    TaskResponse create(UUID projectId, TaskRequest request) {
        Project project = requireProject(projectId);

        Task task = Task.builder()
                .project(project)
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .dueDate(request.dueDate())
                .tags(resolveTags(request.tagNames()))
                .build();

        return TaskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    TaskResponse update(UUID projectId, UUID taskId, TaskRequest request) {
        Task task = findTaskInProjectOrThrow(projectId, taskId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setTags(resolveTags(request.tagNames()));

        return TaskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    void delete(UUID projectId, UUID taskId) {
        Task task = findTaskInProjectOrThrow(projectId, taskId);
        taskRepository.delete(task);
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    }

    private Task findTaskInProjectOrThrow(UUID projectId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        if (!task.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames == null) {
            return new HashSet<>();
        }
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
            tags.add(tag);
        }
        return tags;
    }
}
