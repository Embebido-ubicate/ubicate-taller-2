package com.georoute.feature.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.georoute.feature.user.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Password is required")
    @JsonProperty("password")
    private String password;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("expectedRole")
    private Role expectedRole;

    @JsonProperty("deviceInfo")
    private String deviceInfo;

    @JsonProperty("appVersion")
    private String appVersion;
}