package com.mangarec.features.auth.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@Schema(name = "AuthUserResponse")
public class AuthUserResponse {
    private UUID id;
    private String email;
    private String name;
    private String role;
    private String status;
    private String subscriptionStatus;
    private boolean emailVerified;
    private Instant createdAt;
}
