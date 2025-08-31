package com.georoute.feature.user.dto;

import com.georoute.feature.user.model.User;
import com.georoute.feature.user.enums.Role;
import com.georoute.feature.user.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private UserStatus status;
    private Double latitude;
    private Double longitude;
    private Boolean shareLocation;
    private Long companyId;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .shareLocation(user.getShareLocation())
                .companyId(user.getCompanyId())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Método auxiliar para mantener compatibilidad
    public String getFullName() {
        return firstName + " " + lastName;
    }
}