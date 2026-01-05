package mmi.osaas.txlforma.config;

import lombok.extern.slf4j.Slf4j;
import mmi.osaas.txlforma.enums.Role;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@Slf4j
public class AdminInitializer {

    @Value("${app.default-admin.password:password123}")
    private String defaultPassword;

    @Value("${app.default-admin.email:admin@txlforma.fr}")
    private String defaultEmail;

    @Value("${app.default-admin.firstname:Admin}")
    private String defaultFirstname;

    @Value("${app.default-admin.lastname:User}")
    private String defaultLastname;

    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                                PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                boolean adminExists = userRepository.existsByRole(Role.ADMIN);
                if (!adminExists) {
                    User admin = User.builder()
                            .password(passwordEncoder.encode(defaultPassword))
                            .email(defaultEmail)
                            .firstname(defaultFirstname)
                            .lastname(defaultLastname)
                            .role(Role.ADMIN)
                            .createdAt(LocalDateTime.now())
                            .build();
                    userRepository.save(admin);
                    log.info("[AdminInitializer] Default admin user created: {}", defaultEmail);
                } else {
                    log.info("[AdminInitializer] Admin user already exists, skipping creation.");
                }
            } catch (Exception e) {
                log.error("[AdminInitializer] Failed to ensure default admin exists", e);
            }
        };
    }
}
