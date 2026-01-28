package org.example.authapp.config;

import lombok.AllArgsConstructor;
import org.example.authapp.entity.User;
import org.example.authapp.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
public class InitialDataLoader implements ApplicationRunner {
    private UserRepository userRepository;

    public void run(ApplicationArguments args) {
        if(userRepository.count() == 0) {
            User user = new User();
            user.setUsername("testUser");
            user.setPassword("$2a$12$lpOZZIYH/K6FWilf3pvW3OvQobRoyNj3U8ME3NwXjqUTBMHL5V7Yu");
            user.setEmail("testEmail@test.com");
            user.setFirstName("testFirstName");
            user.setLastName("testLastName");
            user.setEnabled(true);
            Set<String> roles = new HashSet<>();
            roles.add("USER");
            user.setRoles(roles);
            userRepository.save(user);

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("$2a$12$cQmydCcVLPYfF6k0mcyUjuOFXkbkjpNtc8SbLQDZTxtijwNI0tKOu");
            admin.setEmail("admin@admin.com");
            admin.setFirstName("adminFirstName");
            admin.setLastName("adminLastName");
            admin.setEnabled(true);
            roles = new HashSet<>();
            roles.add("ADMIN");
            admin.setRoles(roles);
            userRepository.save(admin);
        }
    }
}
