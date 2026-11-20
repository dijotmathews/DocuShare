package com.dijo.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a user with a securely hashed password (BR-03: passwords must be
     * non-empty and stored securely).
     */
    @Transactional
    public User createUser(String username, String rawPassword, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("User already exists: " + username);
        }
        return userRepository.save(new User(username.trim(), passwordEncoder.encode(rawPassword), role));
    }

    /** Replaces the user's password with a securely hashed new one. */
    @Transactional
    public void changePassword(User user, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}
