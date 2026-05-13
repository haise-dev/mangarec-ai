package com.mangarec.features.auth.service;

public interface GoogleTokenVerifier {
    GoogleAccount verify(String idToken);
}
