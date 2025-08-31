package com.georoute.feature.auth.dto;

import com.georoute.feature.user.dto.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private UserResponse user;

    @Builder.Default
    private String type = "Bearer";

    // Información adicional para el sistema de transporte
    private String message;
    private Boolean locationUpdated;
}