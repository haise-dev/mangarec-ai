package com.mangarec.features.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ResetPasswordRequest")
public class ResetPasswordRequest {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "OTP must not be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otp;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password must not be blank")
    private String confirmPassword;
}
