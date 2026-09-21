package de.webprojekt.repository;

import de.webprojekt.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import de.webprojekt.entity.User;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    java.util.List<Project> findByProjectManager(User projectManager);
    java.util.List<Project> findByEmployeesContaining(User employee);
    boolean existsByIdAndEmployees_Id(Long projectId, Long employeeId);
}