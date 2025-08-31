package com.georoute.feature.auth.dto;

import com.georoute.feature.user.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain: 1 uppercase, 1 lowercase, 1 number and 1 special character"
    )
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "Invalid phone format")
    private String phone;

    // Campos específicos para geolocalización
    private Double latitude;
    private Double longitude;
    private Boolean shareLocation = true;

    // Para usuarios de empresa
    private Long companyId;

    // Role por defecto será PASSENGER, pero se puede especificar
    private Role role = Role.PASSENGER;
}
