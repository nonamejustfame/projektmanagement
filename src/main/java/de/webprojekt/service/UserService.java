package de.webprojekt.service;

import de.webprojekt.repository.UserRepository;
import org.springframework.stereotype.Service;
import de.webprojekt.entity.User;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public User save(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-Mail-Adresse ist bereits vergeben.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public java.util.List<User> findAll() {
        return userRepository.findAll();
    }
    public void delete(User user) {
        userRepository.delete(user);
    }
}