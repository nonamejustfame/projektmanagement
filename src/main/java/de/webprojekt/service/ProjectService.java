package de.webprojekt.service;

import de.webprojekt.entity.Project;
import de.webprojekt.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import de.webprojekt.entity.User;
import de.webprojekt.repository.UserRepository;
import de.webprojekt.entity.TaskStatus;
import de.webprojekt.entity.Task;
import de.webprojekt.repository.TaskRepository;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }
    public Project save(Project project) {
        return projectRepository.save(project);
    }
    public java.util.List<Project> findAll() {
        return projectRepository.findAll();
    }
    public java.util.List<Project> findByProjectManager(User projectManager) {
        return projectRepository.findByProjectManager(projectManager);
    }
    public java.util.List<Project> findByEmployee(User employee) {
        return projectRepository.findByEmployeesContaining(employee);
    }
    public java.util.Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }
    public int calculateProgress(Project project) {
        long totalTasks = taskRepository.findAll()
                .stream()
                .filter(task -> task.getProject().getId().equals(project.getId()))
                .count();
        if (totalTasks == 0) {
            return 0;
        }
        long completedTasks = taskRepository.findAll()
                .stream()
                .filter(task -> task.getProject().getId().equals(project.getId()))
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();
        return (int) ((completedTasks * 100) / totalTasks);
    }
    public User findProjectManager(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden."));
        if (user.getRole() != de.webprojekt.entity.Role.PROJECT_MANAGER) {
            throw new IllegalArgumentException("Benutzer ist kein Projektleiter");
        }
        return user;
    }
    public Project archive(Project project) {
        if (project.isArchived()) {
            return project;
        }
        project.setArchived(true);
        return projectRepository.save(project);
    }
    public Project addEmployee(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nicht gefunden."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden."));
        if (user.getRole() != de.webprojekt.entity.Role.EMPLOYEE) {
            throw new IllegalArgumentException("Nur Mitarbeiter können einem Projekt zugeordnet werden.");
        }
        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new IllegalArgumentException("Benutzer gehört nicht zum selben Mandant");
        }
        project.getEmployees().add(user);
        return projectRepository.save(project);
    }
    public Project removeEmployee(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projekt nicht gefunden."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden."));
        project.getEmployees().remove(user);
        return projectRepository.save(project);
    }
}