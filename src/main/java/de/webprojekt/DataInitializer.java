package de.webprojekt;

import de.webprojekt.entity.Role;
import de.webprojekt.entity.Tenant;
import de.webprojekt.entity.User;
import de.webprojekt.repository.TenantRepository;
import de.webprojekt.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DataInitializer {
    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;
    @Bean
    CommandLineRunner iniData(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@test.de").isEmpty()) {
                Tenant tenant = new Tenant();
                tenant.setName("Testunternehmen");
                tenant = tenantRepository.save(tenant);

                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail("admin@test.de");
                admin.setPassword(passwordEncoder.encode("adminPassword"));
                admin.setRole(Role.ADMIN);
                admin.setTenant(tenant);

                userRepository.save(admin);
            }
        };
    }
}
