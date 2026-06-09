package com.mangarec.features.guest.service;

import com.mangarec.features.guest.controller.response.GuestSessionResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

public interface GuestService {
    GuestSessionResponse createOrTouchGuest(HttpServletRequest request, Duration sessionTtl);
}
