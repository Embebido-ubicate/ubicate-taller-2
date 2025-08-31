package com.georoute.feature.auth.controller;

import com.georoute.feature.auth.dto.AuthResponse;
import com.georoute.feature.auth.dto.LoginRequest;
import com.georoute.feature.auth.dto.RegisterRequest;
import com.georoute.feature.auth.service.AuthService;
import com.georoute.feature.user.dto.LocationUpdateRequest;
import com.georoute.feature.user.dto.UserResponse;
import com.georoute.feature.user.enums.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        log.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.info("Profile request for user: {}", email);

        UserResponse response = authService.getUserProfile(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/location")
    public ResponseEntity<UserResponse> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.info("Location update request for user: {} to [{}, {}]",
                email, request.getLatitude(), request.getLongitude());

        UserResponse response = authService.updateUserLocation(
                email,
                request.getLatitude(),
                request.getLongitude()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/status")
    public ResponseEntity<UserResponse> updateStatus(@RequestParam UserStatus status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.info("Status update request for user: {} to {}", email, status);

        UserResponse response = authService.updateUserStatus(email, status);
        return ResponseEntity.ok(response);
    }

    // Endpoint para validar token (útil para el frontend)
    @GetMapping("/validate")
    public ResponseEntity<UserResponse> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        UserResponse response = authService.getUserProfile(email);
        return ResponseEntity.ok(response);
    }

    // Endpoint de prueba
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth API is working!");
    }
}