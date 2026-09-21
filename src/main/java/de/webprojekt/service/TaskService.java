package de.webprojekt.service;

import de.webprojekt.entity.Project;
import de.webprojekt.entity.Role;
import de.webprojekt.entity.Task;
import de.webprojekt.entity.TaskStatus;
import de.webprojekt.entity.User;
import de.webprojekt.repository.TaskRepository;
import de.webprojekt.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import de.webprojekt.repository.ProjectRepository;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    public Task create(Task task, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Benutzer nicht gefunden."));

        if (user.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException(
                    "Nur Mitarbeiter können Aufgaben erstellen.");
        }

        Project project = task.getProject();

        if (project == null) {
            throw new IllegalArgumentException(
                    "Aufgabe muss einem Projekt zugeordnet sein.");
        }
        if (project.isArchived()) {
            throw new IllegalArgumentException(
                    "Projekt ist archiviert.");
        }

        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new IllegalArgumentException(
                    "Benutzer gehört nicht zum selben Mandant.");
        }

        boolean isMember = projectRepository.existsByIdAndEmployees_Id(project.getId(), user.getId());

        if (!isMember) {
            throw new IllegalArgumentException(
                    "Mitarbeiter ist diesem Projekt nicht zugeordnet.");
        }

        task.setStatus(TaskStatus.OPEN);
        task.setEmployee(null);

        return taskRepository.save(task);
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    private boolean isEmployeeInProject(Task task, User user) {
        return task.getProject()
                .getEmployees()
                .stream()
                .anyMatch(employee ->
                        employee.getId().equals(user.getId()));
    }

    public Task takeTask(Long taskId, Long userId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aufgabe nicht gefunden."));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Benutzer nicht gefunden."));

        if (user.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException(
                    "Nur Mitarbeiter können Aufgaben übernehmen.");
        }

        if (!task.getProject().getTenant().getId()
                .equals(user.getTenant().getId())) {

            throw new IllegalArgumentException(
                    "Benutzer gehört nicht zum selben Mandant.");
        }

        if (!isEmployeeInProject(task, user)) {
            throw new IllegalArgumentException(
                    "Mitarbeiter ist diesem Projekt nicht zugeordnet.");
        }

        if (task.getProject().isArchived()) {
            throw new IllegalArgumentException(
                    "Aufgaben eines archivierten Projekts können nicht übernommen werden.");
        }

        if (task.getStatus() != TaskStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Aufgabe kann nicht übernommen werden.");
        }

        task.setEmployee(user);
        task.setStatus(TaskStatus.IN_PROGRESS);

        return taskRepository.save(task);
    }

    public Task completeTask(Long taskId, Long userId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aufgabe nicht gefunden."));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Benutzer nicht gefunden."));

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Aufgabe kann nicht abgeschlossen werden.");
        }

        if (task.getEmployee() == null ||
                !task.getEmployee().getId().equals(user.getId())) {

            throw new IllegalArgumentException(
                    "Nur der zuständige Mitarbeiter kann die Aufgabe abschließen.");
        }

        task.setStatus(TaskStatus.COMPLETED);

        return taskRepository.save(task);
    }

    public Task update(Task task, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Benutzer nicht gefunden."));

        if (user.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException(
                    "Nur Mitarbeiter können Aufgaben bearbeiten.");
        }

        if (!task.getProject().getTenant().getId()
                .equals(user.getTenant().getId())) {

            throw new IllegalArgumentException(
                    "Benutzer gehört nicht zum selben Mandant.");
        }

        if (!isEmployeeInProject(task, user)) {
            throw new IllegalArgumentException(
                    "Mitarbeiter ist diesem Projekt nicht zugeordnet.");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Erledigte Aufgaben können nicht mehr geändert werden.");
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {

            if (task.getEmployee() == null ||
                    !task.getEmployee().getId().equals(user.getId())) {

                throw new IllegalArgumentException(
                        "Nur der zuständige Mitarbeiter kann die Aufgabe bearbeiten.");
            }
        }

        return taskRepository.save(task);
    }

    public List<Task> findByProject(Project project) {
        return taskRepository.findByProject(project);
    }
}