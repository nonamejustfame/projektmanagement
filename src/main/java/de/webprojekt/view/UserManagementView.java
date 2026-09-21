package de.webprojekt.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.webprojekt.entity.Role;
import de.webprojekt.entity.Tenant;
import de.webprojekt.entity.User;
import de.webprojekt.repository.TenantRepository;
import de.webprojekt.service.UserService;
import jakarta.annotation.security.RolesAllowed;

@Route("users")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;
    private final TenantRepository tenantRepository;

    public UserManagementView(UserService userService, TenantRepository tenantRepository) {

        this.userService = userService;
        this.tenantRepository = tenantRepository;

        add(new H1("Benutzerverwaltung"));

        // ---------------------------------------------------------
        // Benutzer-Grid
        // ---------------------------------------------------------

        Grid<User> userGrid = new Grid<>(User.class, false);

        userGrid.addColumn(User::getName)
                .setHeader("Name");

        userGrid.addColumn(User::getEmail)
                .setHeader("E-Mail");

        userGrid.addColumn(user -> user.getRole().name())
                .setHeader("Rolle");

        userGrid.addColumn(user -> user.getTenant() != null ? user.getTenant().getName() : "")
                .setHeader("Mandant");

        userGrid.setItems(userService.findAll());

        // ---------------------------------------------------------
        // Eingabefelder
        // ---------------------------------------------------------

        TextField nameField = new TextField("Name");

        TextField emailField = new TextField("E-Mail");

        PasswordField passwordField = new PasswordField("Passwort");

        ComboBox<Role> roleField = new ComboBox<>("Rolle");

        roleField.setItems(Role.values());
        roleField.setValue(Role.EMPLOYEE);

        // ---------------------------------------------------------
        // Benutzer anlegen
        // ---------------------------------------------------------

        Button createButton = new Button("Benutzer anlegen");

        createButton.addClickListener(event -> {

            String name = nameField.getValue().trim();
            String email = emailField.getValue().trim();
            String password = passwordField.getValue();
            Role role = roleField.getValue();

            // Pflichtfelder prüfen
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {

                Notification.show("Bitte alle Felder ausfüllen.");

                return;
            }

            // Prüfen, ob E-Mail bereits existiert
            if (userService.findByEmail(email).isPresent()) {

                Notification.show("Diese E-Mail-Adresse ist bereits vergeben.");

                return;
            }

            try {

                // Tenant laden
                Tenant tenant = tenantRepository.findAll()
                        .stream()
                        .filter(t -> t.getName().equals("Testunternehmen"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Testunternehmen wurde nicht gefunden."));

                User user = new User();

                user.setName(name);
                user.setEmail(email);
                user.setPassword(password);
                user.setRole(role);
                user.setTenant(tenant);

                userService.save(user);

                // Grid aktualisieren
                userGrid.setItems(userService.findAll());

                // Felder leeren
                nameField.clear();
                emailField.clear();
                passwordField.clear();
                roleField.setValue(Role.EMPLOYEE);

                Notification.show("Benutzer erfolgreich angelegt.");

            } catch (IllegalArgumentException e) {

                Notification.show(e.getMessage());

            } catch (Exception e) {

                Notification.show("Benutzer konnte nicht angelegt werden.");
            }
        });

        // ---------------------------------------------------------
        // Formular
        // ---------------------------------------------------------

        HorizontalLayout form =
                new HorizontalLayout(
                        nameField,
                        emailField,
                        passwordField,
                        roleField,
                        createButton
                );

        // ---------------------------------------------------------
        // Ansicht
        // ---------------------------------------------------------

        add(form);
        add(userGrid);
    }
}