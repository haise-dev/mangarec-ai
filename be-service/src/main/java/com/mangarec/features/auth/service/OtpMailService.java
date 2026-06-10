package com.mangarec.features.auth.service;

public interface OtpMailService {
    void sendPasswordResetOtp(String email, String name, String otp);

    void sendEmailVerificationOtp(String email, String name, String otp);
}
