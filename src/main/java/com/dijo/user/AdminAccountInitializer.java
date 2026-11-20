package com.dijo.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures an admin account exists (UC-001 precondition). Credentials come from
 * {@code app.admin.username} / {@code app.admin.password}; an account is only
 * created when no ADMIN user exists yet.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final String username;
    private final String password;

    public AdminAccountInitializer(UserRepository userRepository, UserService userService,
                                   @Value("${app.admin.username}") String username,
                                   @Value("${app.admin.password}") String password) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        userService.createUser(username, password, Role.ADMIN);
        log.info("Created initial admin account '{}'", username);
    }
}
