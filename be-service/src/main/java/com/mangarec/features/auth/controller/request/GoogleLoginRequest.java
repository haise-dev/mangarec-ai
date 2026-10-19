package com.mangarec.features.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "GoogleLoginRequest")
public class GoogleLoginRequest {
    @NotBlank(message = "Google id token must not be blank")
    private String idToken;

    @Size(max = 100, message = "Guest id must be at most 100 characters")
    private String guestId;

    @Schema(description = "Optional client device id for refresh token audit")
    private String deviceId;
}
