package de.webprojekt.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.webprojekt.entity.Project;
import de.webprojekt.entity.Role;
import de.webprojekt.entity.User;
import de.webprojekt.service.CurrentUserService;
import de.webprojekt.service.ProjectService;
import de.webprojekt.service.UserService;
import jakarta.annotation.security.RolesAllowed;

@Route("projects")
@RolesAllowed("PROJECT_MANAGER")
public class ProjectManagementView extends VerticalLayout {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    private Project editingProject;

    public ProjectManagementView(ProjectService projectService, CurrentUserService currentUserService, UserService userService) {

        this.projectService = projectService;
        this.currentUserService = currentUserService;
        this.userService = userService;

        User currentUser = currentUserService.getCurrentUser();

        // Eingabefelder
        TextField nameField = new TextField("Projektname");
        TextArea descriptionField = new TextArea("Beschreibung");

        // Mitarbeiter-Auswahl
        MultiSelectComboBox<User> employeeField = new MultiSelectComboBox<>("Mitarbeiter");

        employeeField.setItems(userService.findAll().stream().filter(user -> user.getRole() == Role.EMPLOYEE).toList());
        employeeField.setItemLabelGenerator(User::getName);

        // Projekt-Grid
        Grid<Project> projectGrid = new Grid<>(Project.class, false);

        projectGrid.addColumn(project -> project.getId() + " - " + project.getName()).setHeader("Projekt");

        projectGrid.addColumn(Project::getDescription).setHeader("Beschreibung");

        projectGrid.addColumn(project -> project.isArchived() ? "Archiviert" : "Aktiv").setHeader("Status");

        projectGrid.addColumn(project -> projectService.calculateProgress(project) + "%").setHeader("Fortschritt");

        projectGrid.setItems(projectService.findByProjectManager(currentUser));

        // Projekt anlegen
        Button createButton = new Button("Projekt anlegen");

        createButton.addClickListener(event -> {

            Project project = new Project();

            project.setName(nameField.getValue());
            project.setDescription(descriptionField.getValue());
            project.setArchived(false);

            project.setProjectManager(currentUser);
            project.setTenant(currentUser.getTenant());
            project.setEmployees(employeeField.getValue());

            projectService.save(project);

            projectGrid.setItems(projectService.findByProjectManager(currentUser));

            nameField.clear();
            descriptionField.clear();
            employeeField.clear();
        });

        // Bereich für Projektbearbeitung
        VerticalLayout editSection = new VerticalLayout();

        // Projekt speichern
        Button saveButton = new Button("Projekt speichern");

        saveButton.addClickListener(event -> {

            if (editingProject == null) {
                return;
            }

            editingProject.setName(nameField.getValue());
            editingProject.setDescription(descriptionField.getValue());
            editingProject.setEmployees(employeeField.getValue());

            projectService.save(editingProject);

            projectGrid.setItems(projectService.findByProjectManager(currentUser));

            editingProject = null;

            editSection.setVisible(false);
            createButton.setEnabled(true);

            nameField.clear();
            descriptionField.clear();
            employeeField.clear();
        });

        editSection.add(saveButton);
        editSection.setVisible(false);

        // Projektformular
        HorizontalLayout form = new HorizontalLayout(
                nameField,
                descriptionField,
                createButton
        );

        // Aktionen im Projekt-Grid
        projectGrid.addComponentColumn(project -> {

            if (project.isArchived()) {
                return new Button("Archiviert");
            }

            Button editButton = new Button("Bearbeiten");

            editButton.addClickListener(event -> {

                editingProject = project;

                nameField.setValue(project.getName());
                descriptionField.setValue(project.getDescription());
                employeeField.setValue(project.getEmployees());

                editSection.setVisible(true);
                createButton.setEnabled(false);
            });

            Button archiveButton = new Button("Archivieren");

            archiveButton.addClickListener(event -> {

                projectService.archive(project);

                projectGrid.setItems(projectService.findByProjectManager(currentUser));
            });

            return new HorizontalLayout(
                    editButton,
                    archiveButton
            );

        }).setHeader("Aktion");

        // Ansicht aufbauen
        add(new H1("Projektverwaltung"));
        add(form);
        add(employeeField);
        add(editSection);
        add(projectGrid);
    }
}