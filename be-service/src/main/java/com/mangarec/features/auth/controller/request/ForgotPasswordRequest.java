package com.mangarec.features.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ForgotPasswordRequest")
public class ForgotPasswordRequest {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    @Schema(example = "reader@example.com")
    private String email;
}
