package de.webprojekt.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.webprojekt.entity.Role;
import de.webprojekt.entity.User;
import de.webprojekt.service.CurrentUserService;
import jakarta.annotation.security.RolesAllowed;

@Route("")
@RolesAllowed({"ADMIN", "PROJECT_MANAGER", "EMPLOYEE"})
public class DashboardView extends VerticalLayout {

    private final CurrentUserService currentUserService;

    public DashboardView(CurrentUserService currentUserService, AuthenticationContext authenticationContext) {

        this.currentUserService = currentUserService;

        User currentUser = currentUserService.getCurrentUser();

        add(new H1("Dashboard"));

        // ---------------------------------------------------------
        // Bereich abhängig von der Rolle
        // ---------------------------------------------------------

        if (currentUser.getRole() == Role.ADMIN) {

            add(new Paragraph("Verwalte Benutzer und Rollen."));

            Button usersButton = new Button("Benutzerverwaltung");

            usersButton.addClickListener(event -> usersButton.getUI().ifPresent(ui -> ui.navigate("users")));

            add(usersButton);

        } else if (currentUser.getRole() == Role.PROJECT_MANAGER) {

            add(new Paragraph("Verwalte deine Projekte."));

            Button projectsButton = new Button("Projektverwaltung");

            projectsButton.addClickListener(event -> projectsButton.getUI().ifPresent(ui -> ui.navigate("projects")));

            add(projectsButton);

        } else if (currentUser.getRole() == Role.EMPLOYEE) {

            add(new Paragraph("Hier findest du deine Projekte und Aufgaben."));

            Button projectsButton = new Button("Meine Projekte");

            projectsButton.addClickListener(event -> projectsButton.getUI().ifPresent(ui -> ui.navigate("my-projects")));

            add(projectsButton);
        }

        // ---------------------------------------------------------
        // Logout
        // ---------------------------------------------------------

        Button logoutButton = new Button("Ausloggen");

        logoutButton.addClickListener(event -> authenticationContext.logout());

        add(logoutButton);
    }
}