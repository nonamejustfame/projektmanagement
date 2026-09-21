package de.webprojekt.repository;

import de.webprojekt.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import de.webprojekt.entity.Project;

public interface TaskRepository extends JpaRepository<Task, Long> {
    java.util.List<Task> findByProject(Project project);
}