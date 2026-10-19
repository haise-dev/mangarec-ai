package com.mangarec.features.chat.service.impl;

import com.mangarec.domain.chat.entity.ChatMessageEntity;
import com.mangarec.domain.chat.entity.ChatSessionEntity;
import com.mangarec.domain.chat.repository.ChatMessageRepository;
import com.mangarec.domain.chat.repository.ChatSessionRepository;
import com.mangarec.domain.guest.entity.GuestProfileEntity;
import com.mangarec.domain.guest.repository.GuestProfileRepository;
import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.domain.user.repository.UserRepository;
import com.mangarec.features.chat.controller.request.ChatRequest;
import com.mangarec.features.chat.controller.response.ChatRecommendationResponse;
import com.mangarec.features.chat.controller.response.ChatResponse;
import com.mangarec.features.chat.service.ChatService;
import com.mangarec.features.guest.GuestSessionConstants;
import com.mangarec.security.AuthenticatedUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RestTemplate restTemplate;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GuestProfileRepository guestProfileRepository;

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request, HttpServletRequest httpRequest) {
        String endpoint = aiServiceUrl + "/api/v1/chat/";

        // 1. Resolve Session
        ChatSessionEntity session = resolveSession(request.getSessionId(), httpRequest);

        // 2. Save User Message
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setSession(session);
        userMsg.setSenderType("USER");
        userMsg.setContent(request.getMessage());
        chatMessageRepository.save(userMsg);

        // 3. Call AI Service
        long startTime = System.currentTimeMillis();
        String answer;
        List<ChatRecommendationResponse> recommendations;
        try {
            Map<String, String> requestBody = Map.of("query", request.getMessage());
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody),
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = responseEntity.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from AI service");
            }

            answer = (String) body.getOrDefault("answer", "No answer provided.");
            List<Map<String, Object>> mangas = (List<Map<String, Object>>) body.getOrDefault("mangas", Collections.emptyList());

            recommendations = mangas.stream().map(m -> {
                String mangaId = (String) m.getOrDefault("manga_id", "unknown-id");
                String title = (String) m.getOrDefault("title", "Unknown Title");
                String reason = (String) m.getOrDefault("description", "Recommended based on your query.");
                return new ChatRecommendationResponse(mangaId, title, reason);
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to communicate with AI service: {}", e.getMessage(), e);
            answer = "Xin lỗi, hiện tại tôi đang gặp khó khăn trong việc kết nối tới hệ thống AI. Vui lòng thử lại sau.";
            recommendations = Collections.emptyList();
        }
        long responseTimeMs = System.currentTimeMillis() - startTime;

        // 4. Save AI Message
        ChatMessageEntity agentMsg = new ChatMessageEntity();
        agentMsg.setSession(session);
        agentMsg.setSenderType("AGENT");
        agentMsg.setContent(answer);
        agentMsg.setResponseTimeMs((int) responseTimeMs);
        chatMessageRepository.save(agentMsg);

        return new ChatResponse(session.getId(), answer, recommendations, Instant.now());
    }

    private ChatSessionEntity resolveSession(UUID requestedSessionId, HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity currentUser = null;
        GuestProfileEntity currentGuest = null;

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof AuthenticatedUser authUser) {
            currentUser = userRepository.getReferenceById(authUser.getId());
        } else {
            String guestIdStr = getGuestId(httpRequest);
            if (StringUtils.hasText(guestIdStr)) {
                currentGuest = guestProfileRepository.findByGuestId(guestIdStr).orElse(null);
            }
            if (currentGuest == null) {
                throw new RuntimeException("Cannot resolve User or Guest identity");
            }
        }

        if (requestedSessionId != null) {
            ChatSessionEntity existingSession = chatSessionRepository.findById(requestedSessionId).orElse(null);
            if (existingSession != null) {
                // Verify ownership
                if (currentUser != null && existingSession.getUser() != null && existingSession.getUser().getId().equals(currentUser.getId())) {
                    return existingSession;
                }
                if (currentGuest != null && existingSession.getGuestProfile() != null && existingSession.getGuestProfile().getId().equals(currentGuest.getId())) {
                    return existingSession;
                }
            }
        }

        // Create new session
        ChatSessionEntity newSession = new ChatSessionEntity();
        if (currentUser != null) {
            newSession.setUser(currentUser);
        } else {
            newSession.setGuestProfile(currentGuest);
        }
        newSession.setTitle("Chat Session");
        return chatSessionRepository.save(newSession);
    }

    private String getGuestId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (GuestSessionConstants.COOKIE_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue().trim();
                }
            }
        }
        String headerGuestId = request.getHeader(GuestSessionConstants.HEADER_NAME);
        return StringUtils.hasText(headerGuestId) ? headerGuestId.trim() : null;
    }
}
