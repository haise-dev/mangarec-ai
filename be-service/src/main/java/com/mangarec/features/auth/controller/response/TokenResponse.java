package com.mangarec.features.auth.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(name = "TokenResponse")
public class TokenResponse {
    @Schema(example = "Bearer")
    private String tokenType;

    private String accessToken;
    private String refreshToken;
    private Instant accessTokenExpiresAt;
    private AuthUserResponse user;
}
