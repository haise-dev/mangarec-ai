package com.mangarec.features.auth.service.impl;

import com.mangarec.common.util.RequestHashUtils;
import com.mangarec.domain.auth.entity.AuthActionTokenEntity;
import com.mangarec.domain.auth.entity.UserAuthEventEntity;
import com.mangarec.domain.auth.model.AuthActionPurpose;
import com.mangarec.domain.auth.model.AuthEventType;
import com.mangarec.domain.auth.repository.AuthActionTokenRepository;
import com.mangarec.domain.auth.repository.UserAuthEventRepository;
import com.mangarec.domain.chat.repository.ChatSessionRepository;
import com.mangarec.domain.guest.entity.GuestProfileEntity;
import com.mangarec.domain.guest.repository.GuestProfileRepository;
import com.mangarec.domain.user.entity.UserAuthIdentityEntity;
import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.domain.user.entity.UserPreferenceEntity;
import com.mangarec.domain.user.entity.UserRefreshTokenEntity;
import com.mangarec.domain.user.model.AuthProvider;
import com.mangarec.domain.user.model.SubscriptionStatus;
import com.mangarec.domain.user.model.UserRole;
import com.mangarec.domain.user.model.UserStatus;
import com.mangarec.domain.user.repository.UserAuthIdentityRepository;
import com.mangarec.domain.user.repository.UserPreferenceRepository;
import com.mangarec.domain.user.repository.UserRefreshTokenRepository;
import com.mangarec.domain.user.repository.UserRepository;
import com.mangarec.exception.InvalidDataException;
import com.mangarec.exception.UnauthorizedException;
import com.mangarec.features.auth.controller.request.ForgotPasswordRequest;
import com.mangarec.features.auth.controller.request.GoogleLoginRequest;
import com.mangarec.features.auth.controller.request.LoginRequest;
import com.mangarec.features.auth.controller.request.RegisterRequest;
import com.mangarec.features.auth.controller.request.ResetPasswordRequest;
import com.mangarec.features.auth.controller.response.AuthUserResponse;
import com.mangarec.features.auth.controller.response.TokenResponse;
import com.mangarec.features.auth.service.AuthService;
import com.mangarec.features.auth.service.GoogleAccount;
import com.mangarec.features.auth.service.GoogleTokenVerifier;
import com.mangarec.features.auth.service.OtpMailService;
import com.mangarec.features.ratelimit.service.OtpRateLimitService;
import com.mangarec.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository authIdentityRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;
    private final AuthActionTokenRepository actionTokenRepository;
    private final UserAuthEventRepository authEventRepository;
    private final GuestProfileRepository guestProfileRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final OtpMailService otpMailService;
    private final OtpRateLimitService otpRateLimitService;

    @Value("${auth.password-reset.otp-expiry-minutes}")
    private long passwordResetOtpExpiryMinutes;

    @Override
    @Transactional
    public AuthUserResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)
                || authIdentityRepository.existsByProviderAndProviderSubject(AuthProvider.LOCAL, email)) {
            throw new InvalidDataException("Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setSubscriptionStatus(SubscriptionStatus.FREE);
        user.setEmailVerified(false);
        user = userRepository.saveAndFlush(user);

        UserAuthIdentityEntity identity = new UserAuthIdentityEntity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.LOCAL);
        identity.setProviderSubject(email);
        identity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        authIdentityRepository.save(identity);

        createDefaultPreferences(user);
        claimGuestHistoryIfPresent(request.getGuestId(), user, httpRequest);
        saveAuthEvent(user, request.getGuestId(), AuthEventType.REGISTER, true, null, httpRequest);

        return toUserResponse(user);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            saveAuthEvent(null, null, AuthEventType.LOGIN_FAILED, false, "Invalid email or password", httpRequest);
            throw new UnauthorizedException("Invalid email or password");
        } catch (AuthenticationException e) {
            saveAuthEvent(null, null, AuthEventType.LOGIN_FAILED, false, e.getMessage(), httpRequest);
            throw new UnauthorizedException("Invalid email or password");
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            saveAuthEvent(user, null, AuthEventType.LOGIN_FAILED, false, "User is not active", httpRequest);
            throw new UnauthorizedException("User is not active");
        }

        return issueTokens(user, request.getDeviceId(), httpRequest, AuthEventType.LOGIN_SUCCESS);
    }

    @Override
    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        GoogleAccount googleAccount = googleTokenVerifier.verify(request.getIdToken());
        UserEntity user = findOrCreateGoogleUser(googleAccount);

        if (user.getStatus() != UserStatus.ACTIVE) {
            saveAuthEvent(user, request.getGuestId(), AuthEventType.LOGIN_FAILED, false, "User is not active", httpRequest);
            throw new UnauthorizedException("User is not active");
        }

        claimGuestHistoryIfPresent(request.getGuestId(), user, httpRequest);
        return issueTokens(user, request.getDeviceId(), httpRequest, AuthEventType.GOOGLE_LOGIN);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        otpRateLimitService.checkForgotPassword(email, httpRequest);

        Optional<UserEntity> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            return;
        }

        UserEntity user = userOptional.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        Instant now = Instant.now();
        actionTokenRepository.markActiveTokensUsed(user, AuthActionPurpose.PASSWORD_RESET, now, now);

        String otp = generateOtp();
        AuthActionTokenEntity token = new AuthActionTokenEntity();
        token.setUser(user);
        token.setPurpose(AuthActionPurpose.PASSWORD_RESET);
        token.setTokenHash(hashOtp(email, otp));
        token.setExpiresAt(now.plusSeconds(passwordResetOtpExpiryMinutes * 60));
        actionTokenRepository.save(token);

        otpMailService.sendPasswordResetOtp(user.getEmail(), user.getName(), otp);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidDataException("Confirm password does not match");
        }

        String email = normalizeEmail(request.getEmail());
        AuthActionTokenEntity token = actionTokenRepository
                .findByPurposeAndTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        AuthActionPurpose.PASSWORD_RESET,
                        hashOtp(email, request.getOtp()),
                        Instant.now()
                )
                .orElseThrow(() -> new InvalidDataException("OTP is invalid or expired"));

        UserEntity user = token.getUser();
        if (!email.equals(normalizeEmail(user.getEmail()))) {
            throw new InvalidDataException("OTP is invalid or expired");
        }

        UserAuthIdentityEntity localIdentity = authIdentityRepository
                .findByUserAndProvider(user, AuthProvider.LOCAL)
                .orElseGet(() -> newLocalIdentity(user, email));
        localIdentity.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        authIdentityRepository.save(localIdentity);

        token.setUsedAt(Instant.now());
        actionTokenRepository.save(token);

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    private void claimGuestHistoryIfPresent(String guestId, UserEntity user, HttpServletRequest request) {
        if (!StringUtils.hasText(guestId)) {
            return;
        }

        String ipHash = RequestHashUtils.sha256(RequestHashUtils.clientIp(request));
        String userAgentHash = RequestHashUtils.sha256(RequestHashUtils.userAgent(request));
        GuestProfileEntity guestProfile = guestProfileRepository.findByGuestId(guestId.trim())
                .orElseGet(() -> {
                    GuestProfileEntity newProfile = new GuestProfileEntity();
                    newProfile.setGuestId(guestId.trim());
                    newProfile.setFirstIpHash(ipHash);
                    newProfile.setLastSeenAt(Instant.now());
                    return newProfile;
                });

        guestProfile.setLastIpHash(ipHash);
        guestProfile.setUserAgentHash(userAgentHash);
        guestProfile.setClaimedByUser(user);
        guestProfile.setClaimedAt(Instant.now());
        guestProfile.setLastSeenAt(Instant.now());
        guestProfile = guestProfileRepository.save(guestProfile);

        chatSessionRepository.claimGuestSessions(guestProfile.getId(), user.getId());
    }

    private UserEntity findOrCreateGoogleUser(GoogleAccount googleAccount) {
        return authIdentityRepository
                .findByProviderAndProviderSubject(AuthProvider.GOOGLE, googleAccount.subject())
                .map(UserAuthIdentityEntity::getUser)
                .orElseGet(() -> linkGoogleIdentity(googleAccount));
    }

    private UserEntity linkGoogleIdentity(GoogleAccount googleAccount) {
        UserEntity user = userRepository.findByEmailIgnoreCase(googleAccount.email())
                .orElseGet(() -> createGoogleUser(googleAccount));

        authIdentityRepository.findByUserAndProvider(user, AuthProvider.GOOGLE)
                .ifPresent(existing -> {
                    if (!existing.getProviderSubject().equals(googleAccount.subject())) {
                        throw new InvalidDataException("This account is already linked to another Google identity");
                    }
                });

        UserAuthIdentityEntity identity = new UserAuthIdentityEntity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.GOOGLE);
        identity.setProviderSubject(googleAccount.subject());
        authIdentityRepository.save(identity);
        return user;
    }

    private UserEntity createGoogleUser(GoogleAccount googleAccount) {
        UserEntity user = new UserEntity();
        user.setEmail(googleAccount.email());
        user.setName(StringUtils.hasText(googleAccount.name()) ? googleAccount.name() : googleAccount.email());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setSubscriptionStatus(SubscriptionStatus.FREE);
        user.setEmailVerified(googleAccount.emailVerified());
        user = userRepository.saveAndFlush(user);
        createDefaultPreferences(user);
        return user;
    }

    private void createDefaultPreferences(UserEntity user) {
        UserPreferenceEntity preferences = new UserPreferenceEntity();
        preferences.setUser(user);
        userPreferenceRepository.save(preferences);
    }

    private void saveRefreshToken(
            UserEntity user,
            String refreshToken,
            String deviceId,
            HttpServletRequest request
    ) {
        UserRefreshTokenEntity token = new UserRefreshTokenEntity();
        token.setUser(user);
        token.setTokenHash(RequestHashUtils.sha256(refreshToken));
        token.setDeviceId(StringUtils.hasText(deviceId) ? deviceId.trim() : null);
        token.setIpHash(RequestHashUtils.sha256(RequestHashUtils.clientIp(request)));
        token.setUserAgentHash(RequestHashUtils.sha256(RequestHashUtils.userAgent(request)));
        token.setExpiresAt(jwtService.getRefreshTokenExpiresAt());
        refreshTokenRepository.save(token);
    }

    private TokenResponse issueTokens(
            UserEntity user,
            String deviceId,
            HttpServletRequest request,
            AuthEventType successEventType
    ) {
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Instant accessTokenExpiresAt = jwtService.getAccessTokenExpiresAt();

        saveRefreshToken(user, refreshToken, deviceId, request);
        saveAuthEvent(user, null, successEventType, true, null, request);

        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .user(toUserResponse(user))
                .build();
    }

    private void saveAuthEvent(
            UserEntity user,
            String guestId,
            AuthEventType eventType,
            boolean success,
            String failureReason,
            HttpServletRequest request
    ) {
        UserAuthEventEntity event = new UserAuthEventEntity();
        event.setUser(user);
        event.setGuestId(StringUtils.hasText(guestId) ? guestId.trim() : null);
        event.setEventType(eventType);
        event.setIpHash(RequestHashUtils.sha256(RequestHashUtils.clientIp(request)));
        event.setUserAgentHash(RequestHashUtils.sha256(RequestHashUtils.userAgent(request)));
        event.setSuccess(success);
        event.setFailureReason(failureReason);
        authEventRepository.save(event);
    }

    private AuthUserResponse toUserResponse(UserEntity user) {
        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .subscriptionStatus(user.getSubscriptionStatus().name())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private UserAuthIdentityEntity newLocalIdentity(UserEntity user, String email) {
        UserAuthIdentityEntity identity = new UserAuthIdentityEntity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.LOCAL);
        identity.setProviderSubject(email);
        return identity;
    }

    private String generateOtp() {
        return "%06d".formatted(OTP_RANDOM.nextInt(1_000_000));
    }

    private String hashOtp(String email, String otp) {
        return RequestHashUtils.sha256(normalizeEmail(email) + ":" + otp);
    }
}
