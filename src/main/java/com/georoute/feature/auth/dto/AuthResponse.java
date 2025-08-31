package com.georoute.feature.auth.dto;

import com.georoute.feature.user.dto.UserResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuthResponse {
    private String token;
    private UserResponse user;

    @Builder.Default
    private String type = "Bearer";

    private String redirectUrl;
    private Map<String, Object> roleData;
    private List<String> permissions;
    private Boolean locationRequired;
    private Boolean locationUpdated;
    private String message;
    private Map<String, Object> preferences;

    @Builder.Default
    private String serverVersion = "1.0.0";

    private Long expiresIn;
    private Map<String, Object> appConfig;
}