package com.georoute.feature.user.controller;

import com.georoute.feature.user.dto.LocationUpdateRequest;
import com.georoute.feature.user.dto.UserResponse;
import com.georoute.feature.user.enums.UserStatus;
import com.georoute.feature.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication auth) {
        UserResponse response = authService.getUserProfile(auth.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/location")
    public ResponseEntity<UserResponse> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            Authentication auth) {

        log.info("Location update for user: {} to [{}, {}]",
                auth.getName(), request.getLatitude(), request.getLongitude());

        UserResponse response = authService.updateUserLocation(
                auth.getName(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/status")
    public ResponseEntity<UserResponse> updateStatus(
            @RequestParam UserStatus status,
            Authentication auth) {

        UserResponse response = authService.updateUserStatus(auth.getName(), status);
        return ResponseEntity.ok(response);
    }
}