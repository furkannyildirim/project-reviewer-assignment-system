package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateUserRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateUserRequest;
import com.sakaryateknokent.reviewerassignment.dto.UserResponse;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Sort DEFAULT_USER_SORT = Sort.by(
            Sort.Order.asc("firstName"),
            Sort.Order.asc("lastName")
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        ensureEmailIsAvailable(
                normalizedEmail,
                "Bu e-posta adresiyle kayıtlı bir kullanıcı zaten var"
        );

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByRole(Role.USER, DEFAULT_USER_SORT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findNormalUserById(id));
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findNormalUserById(id);
        String normalizedEmail = normalizeEmail(request.email());

        if (!user.getEmail().equals(normalizedEmail)) {
            ensureEmailIsAvailable(
                    normalizedEmail,
                    "Bu e-posta adresiyle kayıtlı başka bir kullanıcı zaten var"
            );
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(request.phone().trim());
        user.setActive(request.active());

        return toResponse(user);
    }

    public void deleteUser(Long id) {
        User user = findNormalUserById(id);
        userRepository.delete(user);
    }

    private User findNormalUserById(Long id) {
        return userRepository.findByIdAndRole(id, Role.USER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kullanıcı bulunamadı: " + id
                ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureEmailIsAvailable(String email, String errorMessage) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(errorMessage);
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive()
        );
    }
}