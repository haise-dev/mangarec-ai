package com.mangarec.features.auth.controller;

import com.mangarec.common.response.ApiResponse;
import com.mangarec.features.auth.controller.request.ForgotPasswordRequest;
import com.mangarec.features.auth.controller.request.GoogleLoginRequest;
import com.mangarec.features.auth.controller.request.LoginRequest;
import com.mangarec.features.auth.controller.request.RegisterRequest;
import com.mangarec.features.auth.controller.request.ResendEmailVerificationRequest;
import com.mangarec.features.auth.controller.request.ResetPasswordRequest;
import com.mangarec.features.auth.controller.request.VerifyEmailRequest;
import com.mangarec.features.auth.controller.response.AuthUserResponse;
import com.mangarec.features.auth.controller.response.TokenResponse;
import com.mangarec.features.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Register", description = "Create a local account. guestId is optional for claiming guest history.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthUserResponse> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthUserResponse user = authService.register(request, httpRequest);
        return created("Register successful", user);
    }

    @Operation(summary = "Login", description = "Authenticate by email/password and return JWT tokens.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        TokenResponse tokenResponse = authService.login(request, httpRequest);
        return ok("Login successful", tokenResponse);
    }

    @Operation(summary = "Google login", description = "Authenticate with a Google ID token and return JWT tokens.")
    @PostMapping("/google")
    public ApiResponse<TokenResponse> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        TokenResponse tokenResponse = authService.loginWithGoogle(request, httpRequest);
        return ok("Google login successful", tokenResponse);
    }

    @Operation(summary = "Forgot password", description = "Send a 6-digit OTP to the user's email.")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.forgotPassword(request, httpRequest);
        return ok("If the email exists, an OTP has been sent", null);
    }

    @Operation(summary = "Reset password", description = "Reset local password using the OTP sent by email.")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.resetPassword(request, httpRequest);
        return ok("Password reset successful", null);
    }

    @Operation(summary = "Resend email verification OTP", description = "Send a new OTP for local email verification.")
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendEmailVerification(
            @RequestBody @Valid ResendEmailVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.resendEmailVerification(request, httpRequest);
        return ok("If the account exists and is not verified, an OTP has been sent", null);
    }

    @Operation(summary = "Verify email", description = "Verify a local account email using a 6-digit OTP.")
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(
            @RequestBody @Valid VerifyEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.verifyEmail(request, httpRequest);
        return ok("Email verified successfully", null);
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }

    private <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .data(data)
                .build();
    }
}
