package de.webprojekt.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.webprojekt.entity.Project;
import de.webprojekt.entity.Role;
import de.webprojekt.entity.Task;
import de.webprojekt.entity.TaskStatus;
import de.webprojekt.entity.User;
import de.webprojekt.service.CurrentUserService;
import de.webprojekt.service.ProjectService;
import de.webprojekt.service.TaskService;
import jakarta.annotation.security.RolesAllowed;

@Route("projects/:id")
@RolesAllowed({"PROJECT_MANAGER", "EMPLOYEE"})
public class ProjectDetailView extends VerticalLayout implements BeforeEnterObserver {

    private Long projectId;

    private final ProjectService projectService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    private Task editingTask;

    public ProjectDetailView(ProjectService projectService, TaskService taskService, CurrentUserService currentUserService) {

        this.projectService = projectService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;

        add(new H1("Projekt"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        RouteParameters parameters = event.getRouteParameters();

        if (parameters != null && parameters.get("id").isPresent()) {

            projectId = Long.valueOf(parameters.get("id").get());

            Project project = projectService.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Projekt nicht gefunden"));

            User currentUser = currentUserService.getCurrentUser();

            // Zugriff auf das Projekt prüfen
            boolean hasAccess;

            if (currentUser.getRole() == Role.PROJECT_MANAGER) {

                hasAccess = project.getProjectManager() != null && project.getProjectManager()
                        .getId()
                        .equals(currentUser.getId());
            } else {

                hasAccess = projectService.findByEmployee(currentUser)
                        .stream()
                        .anyMatch(p -> p.getId().equals(project.getId()));
            }

            if (!hasAccess) {
                Notification.show("Kein Zugriff auf dieses Projekt.");
                event.rerouteTo("");
                return;
            }

            removeAll();

            add(new H1(project.getName()));

            // ---------------------------------------------------------
            // Eingabefelder
            // ---------------------------------------------------------

            TextField titleField = new TextField("Aufgabentitel");

            TextArea taskDescriptionField = new TextArea("Beschreibung");

            // ---------------------------------------------------------
            // Buttons
            // ---------------------------------------------------------

            Button createTaskButton = new Button("Aufgabe erstellen");

            Button saveTaskButton = new Button("Aufgabe speichern");

            // Bearbeitungsbereich
            VerticalLayout editSection = new VerticalLayout(saveTaskButton);

            editSection.setVisible(false);

            // ---------------------------------------------------------
            // Aufgaben-Grid
            // ---------------------------------------------------------

            Grid<Task> taskGrid = new Grid<>(Task.class, false);

            taskGrid.addColumn(Task::getTitle)
                    .setHeader("Aufgabe");

            taskGrid.addColumn(Task::getDescription)
                    .setHeader("Beschreibung");

            taskGrid.addColumn(task -> task.getStatus().name())
                    .setHeader("Status");

            taskGrid.addColumn(task -> task.getEmployee() != null ? task.getEmployee().getName() : "Niemand")
                    .setHeader("Mitarbeiter");

            // ---------------------------------------------------------
            // Aktionen
            // ---------------------------------------------------------

            taskGrid.addComponentColumn(task -> {

                // Projektleiter dürfen Aufgaben nur ansehen
                if (currentUser.getRole() != Role.EMPLOYEE) {
                    return new Span("");
                }

                // -----------------------------------------------------
                // Aufgabe ist OPEN
                // -----------------------------------------------------

                if (task.getStatus() == TaskStatus.OPEN) {

                    Button takeButton = new Button("Übernehmen");

                    takeButton.addClickListener(clickEvent -> {

                        taskService.takeTask(task.getId(), currentUser.getId());

                        taskGrid.setItems(taskService.findByProject(project));
                    });

                    Button editButton = new Button("Bearbeiten");

                    editButton.addClickListener(clickEvent -> {

                        editingTask = task;

                        titleField.setValue(task.getTitle());

                        taskDescriptionField.setValue(task.getDescription() != null ? task.getDescription() : "");

                        editSection.setVisible(true);
                        createTaskButton.setEnabled(false);
                    });

                    return new HorizontalLayout(takeButton, editButton);
                }

                // -----------------------------------------------------
                // Aufgabe ist IN_PROGRESS
                // -----------------------------------------------------

                if (task.getStatus() == TaskStatus.IN_PROGRESS && task.getEmployee() != null && task.getEmployee().getId().equals(currentUser.getId())) {

                    Button editButton = new Button("Bearbeiten");

                    editButton.addClickListener(clickEvent -> {

                        editingTask = task;

                        titleField.setValue(task.getTitle());

                        taskDescriptionField.setValue(task.getDescription() != null ? task.getDescription() : "");

                        editSection.setVisible(true);
                        createTaskButton.setEnabled(false);
                    });

                    Button completeButton = new Button("Abschließen");

                    completeButton.addClickListener(clickEvent -> {

                        taskService.completeTask(task.getId(), currentUser.getId());

                        taskGrid.setItems(taskService.findByProject(project));
                    });

                    return new HorizontalLayout(editButton, completeButton);
                }

                // COMPLETED oder Aufgabe gehört einem anderen Mitarbeiter
                return new Span("");

            }).setHeader("Aktion");

            taskGrid.setItems(taskService.findByProject(project));

            // ---------------------------------------------------------
            // Aufgabe erstellen
            // ---------------------------------------------------------

            createTaskButton.addClickListener(clickEvent -> {

                Task task = new Task();

                task.setTitle(titleField.getValue());

                task.setDescription(taskDescriptionField.getValue());

                task.setProject(project);

                taskService.create(task, currentUser.getId());

                taskGrid.setItems(taskService.findByProject(project));

                titleField.clear();
                taskDescriptionField.clear();
            });

            // ---------------------------------------------------------
            // Aufgabe speichern
            // ---------------------------------------------------------

            saveTaskButton.addClickListener(clickEvent -> {

                if (editingTask == null) {
                    return;
                }

                editingTask.setTitle(titleField.getValue());

                editingTask.setDescription(taskDescriptionField.getValue());

                taskService.update(editingTask, currentUser.getId());

                taskGrid.setItems(taskService.findByProject(project));

                editingTask = null;

                editSection.setVisible(false);
                createTaskButton.setEnabled(true);

                titleField.clear();
                taskDescriptionField.clear();
            });

            // ---------------------------------------------------------
            // Formular
            // ---------------------------------------------------------

            HorizontalLayout taskForm = new HorizontalLayout(titleField, taskDescriptionField, createTaskButton);

            // Nur Mitarbeiter bekommen das Aufgabenformular
            if (currentUser.getRole() == Role.EMPLOYEE) {
                add(taskForm);
                add(editSection);
            }

            add(taskGrid);
        }
    }
}