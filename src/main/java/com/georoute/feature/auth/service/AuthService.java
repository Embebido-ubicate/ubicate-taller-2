package com.georoute.feature.auth.service;

import com.georoute.feature.auth.dto.AuthResponse;
import com.georoute.feature.auth.dto.LoginRequest;
import com.georoute.feature.auth.dto.RegisterRequest;
import com.georoute.feature.user.dto.UserResponse;
import com.georoute.feature.user.model.User;
import com.georoute.feature.user.repository.UserRepository;
import com.georoute.feature.user.enums.Role;
import com.georoute.feature.user.enums.UserStatus;
import com.georoute.feature.route.repository.RouteRepository;
import com.georoute.feature.tracking.repository.DriverTrackingRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    // Constante para evitar duplicar el literal
    private static final String USER_NOT_FOUND_MESSAGE = "User not found: ";

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final DriverTrackingRepository trackingRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        // Verificar si el usuario ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
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

        // Respuesta mejorada según el rol
        return buildRoleBasedResponse(token, savedUser,
                request.getLatitude() != null && request.getLongitude() != null, "Registration successful");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Starting login process for email: {}", request.getEmail());

        try {
            // Autenticar usuario
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Obtener usuario
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Verificar que la cuenta esté activa
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new InactiveAccountException("Account is not active. Status: " + user.getStatus());
            }

            // VALIDAR ROL ESPERADO (si se especifica)
            if (request.getExpectedRole() != null && user.getRole() != request.getExpectedRole()) {
                throw new BadCredentialsException("Invalid credentials for this role");
            }

            // ACTUALIZAR UBICACIÓN SOLO SI ES NECESARIO
            boolean locationUpdated = updateLocationIfNeeded(user, request);

            // Actualizar último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generar token
            String token = jwtService.getToken(user, user);

            log.info("Login successful for email: {} with role: {}", request.getEmail(), user.getRole());

            // RESPUESTA SEGÚN EL ROL
            return buildRoleBasedResponse(token, user, locationUpdated, "Login successful");

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + email));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserLocation(String email, Double latitude, Double longitude) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + email));

        user.setLatitude(latitude);
        user.setLongitude(longitude);
        User savedUser = userRepository.save(user);

        log.info("Location updated for user: {} to [{}, {}]", email, latitude, longitude);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUserStatus(String email, UserStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MESSAGE + email));

        user.setStatus(status);
        User savedUser = userRepository.save(user);

        log.info("Status updated for user: {} to {}", email, status);
        return UserResponse.from(savedUser);
    }

    // ========== MÉTODOS PRIVADOS PARA LOGIN POR ROLES ==========

    private boolean updateLocationIfNeeded(User user, LoginRequest request) {
        // Solo actualizar ubicación para DRIVERS y PASSENGERS
        if ((user.getRole() == Role.DRIVER || user.getRole() == Role.PASSENGER)
                && request.getLatitude() != null && request.getLongitude() != null) {

            user.setLatitude(request.getLatitude());
            user.setLongitude(request.getLongitude());
            log.debug("Location updated for user: {} to [{}, {}]",
                    user.getEmail(), request.getLatitude(), request.getLongitude());
            return true;
        }
        return false;
    }

    private AuthResponse buildRoleBasedResponse(String token, User user, boolean locationUpdated, String message) {
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .message(message)
                .locationRequired(user.getRole() == Role.DRIVER || user.getRole() == Role.PASSENGER);

        // CONFIGURAR RESPUESTA SEGÚN EL ROL
        switch (user.getRole()) {
            case SYSTEM_ADMIN:
                return builder
                        .redirectUrl("/admin/dashboard")
                        .roleData(getSystemAdminData())
                        .permissions(List.of("MANAGE_COMPANIES", "MANAGE_USERS", "VIEW_ALL_DATA", "APPROVE_COMPANIES"))
                        .build();

            case COMPANY_ADMIN:
                return builder
                        .redirectUrl("/company/dashboard")
                        .roleData(getCompanyAdminData(user.getCompanyId()))
                        .permissions(List.of("MANAGE_ROUTES", "MANAGE_BUSES", "VIEW_TRACKING", "MANAGE_DRIVERS"))
                        .build();

            case DRIVER:
                return builder
                        .redirectUrl("/driver/dashboard")
                        .roleData(getDriverData(user.getId()))
                        .permissions(List.of("START_TRIP", "SEND_LOCATION", "VIEW_ROUTE", "UPDATE_TRIP_STATUS"))
                        .build();

            case PASSENGER:
                return builder
                        .redirectUrl("/passenger/search")
                        .roleData(getPassengerData(user.getId(), user.getLatitude(), user.getLongitude()))
                        .permissions(List.of("SEARCH_ROUTES", "VIEW_TRACKING", "SAVE_FAVORITES"))
                        .build();

            default:
                return builder
                        .redirectUrl("/dashboard")
                        .roleData(new HashMap<>())
                        .permissions(new ArrayList<>())
                        .build();
        }
    }

    // ========== DATOS ESPECÍFICOS POR ROL ==========

    private Map<String, Object> getSystemAdminData() {
        Map<String, Object> data = new HashMap<>();

        try {
            data.put("totalUsers", userRepository.count());
            data.put("activeUsers", userRepository.countActiveUsers());
            data.put("totalRoutes", routeRepository.count());
            data.put("activeRoutes", routeRepository.countActiveRoutes());
            data.put("systemStatus", "OPERATIONAL");
            data.put("lastUpdate", LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Error getting system admin data: {}", e.getMessage());
            data.put("error", "Could not load statistics");
        }

        return data;
    }

    private Map<String, Object> getCompanyAdminData(Long companyId) {
        Map<String, Object> data = new HashMap<>();

        if (companyId != null) {
            try {
                data.put("companyId", companyId);
                data.put("totalRoutes", routeRepository.countByCompanyIdAndActiveTrue(companyId));
                data.put("totalDrivers", userRepository.countByRole(Role.DRIVER));
                data.put("activeTrips", trackingRepository.countByStatus(
                        com.georoute.feature.tracking.enums.TrackingStatus.STARTED));
            } catch (Exception e) {
                log.warn("Error getting company admin data for company {}: {}", companyId, e.getMessage());
                data.put("error", "Could not load company statistics");
            }
        } else {
            data.put("needsCompanyAssignment", true);
            data.put("message", "Contact administrator to assign company");
        }

        return data;
    }

    private Map<String, Object> getDriverData(Long userId) {
        Map<String, Object> data = new HashMap<>();

        try {
            var activeTrip = trackingRepository.findActiveByDriverId(userId);
            data.put("hasActiveTrip", activeTrip.isPresent());
            data.put("canStartTrip", !activeTrip.isPresent());

            if (activeTrip.isPresent()) {
                var trip = activeTrip.get();
                data.put("currentTrip", Map.of(
                        "tripId", trip.getId(),
                        "routeId", trip.getRouteId(),
                        "status", trip.getStatus().name(),
                        "startTime", trip.getStartTime(),
                        "elapsedTime", trip.getFormattedElapsedTime(),
                        "totalDistance", trip.getTotalDistanceKm() != null ? trip.getTotalDistanceKm() : 0.0,
                        "currentSpeed", trip.getSpeed() != null ? trip.getSpeed() : 0.0
                ));
            }
        } catch (Exception e) {
            log.warn("Error getting driver data for user {}: {}", userId, e.getMessage());
            data.put("hasActiveTrip", false);
            data.put("canStartTrip", true);
        }

        return data;
    }

    private Map<String, Object> getPassengerData(Long userId, Double latitude, Double longitude) {
        Map<String, Object> data = new HashMap<>();

        try {
            if (latitude != null && longitude != null) {
                data.put("hasLocation", true);
                data.put("nearbyRoutesCount", 0); // TODO: Implementar búsqueda de rutas cercanas
            } else {
                data.put("hasLocation", false);
                data.put("needsLocation", true);
            }

            data.put("favoriteRoutesCount", 0);
            data.put("recentSearchesCount", 0);
            data.put("suggestions", List.of(
                    "Enable location for better route suggestions",
                    "Save frequently used routes as favorites"
            ));

        } catch (Exception e) {
            log.warn("Error getting passenger data for user {}: {}", userId, e.getMessage());
            data.put("hasLocation", false);
        }

        return data;
    }

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InactiveAccountException extends RuntimeException {
        public InactiveAccountException(String message) {
            super(message);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}