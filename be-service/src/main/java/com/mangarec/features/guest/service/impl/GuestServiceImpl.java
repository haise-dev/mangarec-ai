package com.mangarec.features.guest.service.impl;

import com.mangarec.common.util.RequestHashUtils;
import com.mangarec.domain.guest.entity.GuestProfileEntity;
import com.mangarec.domain.guest.repository.GuestProfileRepository;
import com.mangarec.features.guest.GuestSessionConstants;
import com.mangarec.features.guest.controller.response.GuestSessionResponse;
import com.mangarec.features.guest.service.GuestService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {
    private final GuestProfileRepository guestProfileRepository;

    @Override
    @Transactional
    public GuestSessionResponse createOrTouchGuest(HttpServletRequest request, Duration sessionTtl) {
        String existingGuestId = guestId(request);
        GuestProfileEntity guestProfile = StringUtils.hasText(existingGuestId)
                ? guestProfileRepository.findByGuestId(existingGuestId).orElseGet(() -> newGuestProfile(request))
                : newGuestProfile(request);

        touch(guestProfile, request);
        guestProfile = guestProfileRepository.save(guestProfile);

        return new GuestSessionResponse(
                guestProfile.getGuestId(),
                GuestSessionConstants.COOKIE_NAME,
                Instant.now().plus(sessionTtl)
        );
    }

    private GuestProfileEntity newGuestProfile(HttpServletRequest request) {
        String ipHash = RequestHashUtils.sha256(RequestHashUtils.clientIp(request));
        GuestProfileEntity guestProfile = new GuestProfileEntity();
        guestProfile.setGuestId(UUID.randomUUID().toString());
        guestProfile.setFirstIpHash(ipHash);
        guestProfile.setLastSeenAt(Instant.now());
        return guestProfile;
    }

    private void touch(GuestProfileEntity guestProfile, HttpServletRequest request) {
        guestProfile.setLastIpHash(RequestHashUtils.sha256(RequestHashUtils.clientIp(request)));
        guestProfile.setUserAgentHash(RequestHashUtils.sha256(RequestHashUtils.userAgent(request)));
        guestProfile.setLastSeenAt(Instant.now());
    }

    private String guestId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (GuestSessionConstants.COOKIE_NAME.equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue().trim();
                }
            }
        }

        String headerGuestId = request.getHeader(GuestSessionConstants.HEADER_NAME);
        return StringUtils.hasText(headerGuestId) ? headerGuestId.trim() : null;
    }
}
