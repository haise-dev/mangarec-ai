package com.mangarec.features.auth.service;

import com.mangarec.features.auth.controller.request.LoginRequest;
import com.mangarec.features.auth.controller.request.ForgotPasswordRequest;
import com.mangarec.features.auth.controller.request.GoogleLoginRequest;
import com.mangarec.features.auth.controller.request.RegisterRequest;
import com.mangarec.features.auth.controller.request.ResendEmailVerificationRequest;
import com.mangarec.features.auth.controller.request.ResetPasswordRequest;
import com.mangarec.features.auth.controller.request.VerifyEmailRequest;
import com.mangarec.features.auth.controller.response.AuthUserResponse;
import com.mangarec.features.auth.controller.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthUserResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    TokenResponse login(LoginRequest request, HttpServletRequest httpRequest);

    TokenResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest);

    void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest);

    void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest);

    void resendEmailVerification(ResendEmailVerificationRequest request, HttpServletRequest httpRequest);

    void verifyEmail(VerifyEmailRequest request, HttpServletRequest httpRequest);
}
