package com.taskhub.taskservice.slice;

import com.taskhub.taskservice.TestcontainersConfiguration;
import com.taskhub.taskservice.project.Project;
import com.taskhub.taskservice.tag.Tag;
import com.taskhub.taskservice.task.Task;
import com.taskhub.taskservice.task.TaskStatus;
import com.taskhub.taskservice.user.Role;
import com.taskhub.taskservice.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
class EntityPersistenceTests {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void should_roundTripFullEntityGraph_when_persistingUserProjectTaskAndTags() {
        User owner = entityManager.persistFlushFind(
                User.builder()
                        .email("owner@taskhub.dev")
                        .passwordHash("hashed-password")
                        .role(Role.USER)
                        .build());

        Project project = entityManager.persistFlushFind(
                Project.builder()
                        .name("Launch")
                        .description("Launch project")
                        .owner(owner)
                        .build());

        Tag tag = entityManager.persistFlushFind(
                Tag.builder().name("urgent").build());

        Task task = Task.builder()
                .project(project)
                .title("Ship it")
                .status(TaskStatus.TODO)
                .tags(new HashSet<>(Set.of(tag)))
                .build();
        Task persistedTask = entityManager.persistFlushFind(task);

        entityManager.clear();

        Task reloaded = entityManager.find(Task.class, persistedTask.getId());

        assertThat(reloaded.getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getProject().getOwner().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getTags()).extracting(Tag::getName).containsExactly("urgent");
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
}
