package com.mangarec.features.auth.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mangarec.exception.InvalidDataException;
import com.mangarec.exception.UnauthorizedException;
import com.mangarec.features.auth.service.GoogleAccount;
import com.mangarec.features.auth.service.GoogleTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Locale;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {
    private final String googleClientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierImpl(@Value("${google.auth.client-id}") String googleClientId) {
        this.googleClientId = googleClientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(StringUtils.hasText(googleClientId) ? Collections.singletonList(googleClientId) : null)
                .build();
    }

    @Override
    public GoogleAccount verify(String idToken) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new InvalidDataException("Google client id is not configured");
        }
        if (!StringUtils.hasText(idToken)) {
            throw new UnauthorizedException("Google id token is required");
        }

        try {
            GoogleIdToken verifiedToken = verifier.verify(idToken);
            if (verifiedToken == null) {
                throw new UnauthorizedException("Invalid Google id token");
            }

            GoogleIdToken.Payload payload = verifiedToken.getPayload();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();
            if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
                throw new UnauthorizedException("Google email is not verified");
            }

            return new GoogleAccount(
                    payload.getSubject(),
                    email.trim().toLowerCase(Locale.ROOT),
                    true,
                    (String) payload.get("name")
            );
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new UnauthorizedException("Cannot verify Google id token");
        }
    }
}
