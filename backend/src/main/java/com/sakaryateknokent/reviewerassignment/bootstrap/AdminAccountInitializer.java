package com.sakaryateknokent.reviewerassignment.bootstrap;

import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean adminCreationEnabled = environment.getProperty(
                "APP_ADMIN_ENABLED",
                Boolean.class,
                false
        );

        if (!adminCreationEnabled) {
            return;
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin hesabı zaten mevcut, başlangıç oluşturma işlemi atlandı");
            return;
        }

        String firstName = getRequiredProperty("APP_ADMIN_FIRST_NAME").trim();
        String lastName = getRequiredProperty("APP_ADMIN_LAST_NAME").trim();
        String email = getRequiredProperty("APP_ADMIN_EMAIL")
                .trim()
                .toLowerCase(Locale.ROOT);
        String phone = getRequiredProperty("APP_ADMIN_PHONE").trim();
        String password = getRequiredProperty("APP_ADMIN_PASSWORD");

        validatePasswordLength(password);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Admin e-posta adresi başka bir kullanıcı tarafından kullanılıyor"
            );
        }

        User admin = new User();
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(email);
        admin.setPhone(phone);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);

        log.info("İlk admin hesabı başarıyla oluşturuldu: {}", email);
    }

    private String getRequiredProperty(String propertyName) {
        String value = environment.getProperty(propertyName);

        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Zorunlu ortam değişkeni tanımlı değil: " + propertyName
            );
        }

        return value;
    }

    private void validatePasswordLength(String password) {
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException(
                    "Admin parolası 8 ile 72 karakter arasında olmalıdır"
            );
        }
    }
}