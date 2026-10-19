package com.mangarec.features.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "LoginRequest")
public class LoginRequest {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    @Schema(example = "reader@example.com")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Schema(example = "StrongPass123")
    private String password;

    @Schema(description = "Optional client device id for refresh token audit")
    private String deviceId;
}
