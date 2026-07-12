package com.taskhub.taskservice.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProjectRepository extends JpaRepository<Project, UUID> {
}
