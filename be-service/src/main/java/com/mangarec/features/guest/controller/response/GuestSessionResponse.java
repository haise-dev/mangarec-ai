package com.mangarec.features.guest.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "GuestSessionResponse")
public record GuestSessionResponse(
        @Schema(example = "99a4fdd8-3d3c-43b8-aefd-577f582ed98f")
        String guestId,

        @Schema(example = "mangarec_guest_id")
        String cookieName,

        Instant expiresAt
) {
}
