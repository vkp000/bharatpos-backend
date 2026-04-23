package com.bharatpos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Phone or email is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}