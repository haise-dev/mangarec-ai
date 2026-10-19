package com.mangarec.features.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "RegisterRequest")
public class RegisterRequest {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    @Schema(example = "reader@example.com")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @Schema(example = "StrongPass123")
    private String password;

    @NotBlank(message = "Name must not be blank")
    @Size(max = 255, message = "Name must be at most 255 characters")
    @Schema(example = "Manga Reader")
    private String name;

    @Size(max = 100, message = "Guest id must be at most 100 characters")
    @Schema(description = "Optional guest id used to claim guest chat history after registration")
    private String guestId;
}
