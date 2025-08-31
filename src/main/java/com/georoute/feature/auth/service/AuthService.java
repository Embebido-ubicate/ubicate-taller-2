package com.georoute.feature.auth.service;

import com.georoute.feature.auth.dto.AuthResponse;
import com.georoute.feature.auth.dto.LoginRequest;
import com.georoute.feature.auth.dto.RegisterRequest;
import com.georoute.feature.user.dto.UserResponse;
import com.georoute.feature.user.model.User;
import com.georoute.feature.user.repository.UserRepository;
import com.georoute.feature.user.enums.UserStatus;
import com.georoute.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        // Verificar si el usuario ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }

        // Crear nuevo usuario
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .shareLocation(request.getShareLocation())
                .companyId(request.getCompanyId())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generar token
        String token = jwtService.getToken(savedUser, savedUser);

        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(savedUser))
                .message("Registration successful")
                .locationUpdated(request.getLatitude() != null && request.getLongitude() != null)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Starting login process for email: {}", request.getEmail());

        try {
            // Autenticar usuario
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Obtener usuario
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Verificar que la cuenta esté activa
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new RuntimeException("Account is not active. Status: " + user.getStatus());
            }

            // Actualizar ubicación si se proporciona
            boolean locationUpdated = false;
            if (request.getLatitude() != null && request.getLongitude() != null) {
                user.setLatitude(request.getLatitude());
                user.setLongitude(request.getLongitude());
                locationUpdated = true;
                log.debug("Location updated for user: {} to [{}, {}]",
                        user.getEmail(), request.getLatitude(), request.getLongitude());
            }

            // Actualizar último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generar token
            String token = jwtService.getToken(user, user);

            log.info("Login successful for email: {}", request.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .user(UserResponse.from(user))
                    .message("Login successful")
                    .locationUpdated(locationUpdated)
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserLocation(String email, Double latitude, Double longitude) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.setLatitude(latitude);
        user.setLongitude(longitude);
        User savedUser = userRepository.save(user);

        log.info("Location updated for user: {} to [{}, {}]", email, latitude, longitude);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUserStatus(String email, UserStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.setStatus(status);
        User savedUser = userRepository.save(user);

        log.info("Status updated for user: {} to {}", email, status);
        return UserResponse.from(savedUser);
    }
}