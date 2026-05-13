package com.mangarec.features.auth.service;

public record GoogleAccount(
        String subject,
        String email,
        boolean emailVerified,
        String name
) {
}
