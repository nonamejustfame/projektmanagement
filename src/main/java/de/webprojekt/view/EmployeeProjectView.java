package de.webprojekt.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.component.grid.Grid;
import de.webprojekt.entity.Project;
import de.webprojekt.entity.User;
import de.webprojekt.service.CurrentUserService;
import  de.webprojekt.service.ProjectService;

@Route("my-projects")
@RolesAllowed("EMPLOYEE")
public class EmployeeProjectView extends VerticalLayout {
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public EmployeeProjectView(ProjectService projectService, CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.currentUserService = currentUserService;

        User currentUser = currentUserService.getCurrentUser();

        Grid<Project> projectGrid = new Grid<>(Project.class, false);
        projectGrid.addColumn(Project::getName).setHeader("Projekt");
        projectGrid.addColumn(Project::getDescription).setHeader("Beschreibung");
        projectGrid.addColumn(project -> project.isArchived() ? "Archiviert" : "Aktiv").setHeader("Status");
        projectGrid.setItems(projectService.findByEmployee(currentUser));

        add(new H1("Meine Projekte"));
        add(projectGrid);
    }
}
