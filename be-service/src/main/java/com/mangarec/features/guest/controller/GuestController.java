package com.mangarec.features.guest.controller;

import com.mangarec.common.response.ApiResponse;
import com.mangarec.features.guest.GuestSessionConstants;
import com.mangarec.features.guest.controller.response.GuestSessionResponse;
import com.mangarec.features.guest.service.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
@Tag(name = "Guests")
public class GuestController {
    private static final Duration GUEST_COOKIE_AGE = Duration.ofDays(365);

    private final GuestService guestService;

    @Operation(summary = "Create guest session", description = "Issue a backend-generated guest_id for guest AI quota.")
    @PostMapping
    public ResponseEntity<ApiResponse<GuestSessionResponse>> createGuest(HttpServletRequest request) {
        GuestSessionResponse guestSession = guestService.createOrTouchGuest(request, GUEST_COOKIE_AGE);
        ResponseCookie cookie = ResponseCookie.from(GuestSessionConstants.COOKIE_NAME, guestSession.guestId())
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(GUEST_COOKIE_AGE)
                .build();

        ApiResponse<GuestSessionResponse> body = ApiResponse.<GuestSessionResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Guest session issued")
                .data(guestSession)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }
}
