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
import com.mangarec.features.chat.controller.response.ChatResponse;
import com.mangarec.features.guest.GuestSessionConstants;
import com.mangarec.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GuestProfileRepository guestProfileRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Captor
    private ArgumentCaptor<ChatMessageEntity> messageCaptor;

    @Captor
    private ArgumentCaptor<ChatSessionEntity> sessionCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "aiServiceUrl", "http://localhost:8000");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guestWithoutSessionIdCreatesNewSessionAndSavesMessages() {
        String guestId = "guest-123";
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(GuestSessionConstants.HEADER_NAME, guestId);

        GuestProfileEntity guestProfile = new GuestProfileEntity();
        guestProfile.setId(UUID.randomUUID());
        when(guestProfileRepository.findByGuestId(guestId)).thenReturn(Optional.of(guestProfile));

        ChatSessionEntity newSession = new ChatSessionEntity();
        newSession.setId(UUID.randomUUID());
        newSession.setGuestProfile(guestProfile);
        when(chatSessionRepository.save(any(ChatSessionEntity.class))).thenReturn(newSession);

        Map<String, Object> aiResponseBody = Map.of(
                "answer", "Hello from AI",
                "mangas", Collections.emptyList()
        );
        when(restTemplate.exchange(
                eq("http://localhost:8000/api/v1/chat/"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(aiResponseBody, HttpStatus.OK));

        ChatRequest request = new ChatRequest();
        request.setMessage("Recommend me a manga");

        ChatResponse response = chatService.chat(request, httpRequest);

        // Verify session creation
        verify(chatSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getGuestProfile()).isEqualTo(guestProfile);

        // Verify message saving (User & Agent)
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessageEntity> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages.get(0).getSenderType()).isEqualTo("USER");
        assertThat(savedMessages.get(0).getContent()).isEqualTo("Recommend me a manga");
        assertThat(savedMessages.get(0).getSession()).isEqualTo(newSession);

        assertThat(savedMessages.get(1).getSenderType()).isEqualTo("AGENT");
        assertThat(savedMessages.get(1).getContent()).isEqualTo("Hello from AI");
        assertThat(savedMessages.get(1).getSession()).isEqualTo(newSession);

        // Verify response
        assertThat(response.sessionId()).isEqualTo(newSession.getId());
        assertThat(response.answer()).isEqualTo("Hello from AI");
    }

    @Test
    void userWithExistingSessionReusesSession() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        
        AuthenticatedUser principal = new AuthenticatedUser(userEntity);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(userRepository.getReferenceById(userId)).thenReturn(userEntity);

        UUID sessionId = UUID.randomUUID();
        ChatSessionEntity existingSession = new ChatSessionEntity();
        existingSession.setId(sessionId);
        existingSession.setUser(userEntity);
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(existingSession));

        Map<String, Object> aiResponseBody = Map.of("answer", "Hello User");
        when(restTemplate.exchange(
                any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(aiResponseBody, HttpStatus.OK));

        ChatRequest request = new ChatRequest();
        request.setSessionId(sessionId);
        request.setMessage("Test message");

        ChatResponse response = chatService.chat(request, new MockHttpServletRequest());

        // Verify session reused, NOT saved
        verify(chatSessionRepository, times(0)).save(any());

        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getSession().getId()).isEqualTo(sessionId);
        
        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.answer()).isEqualTo("Hello User");
    }

    @Test
    void aiServiceFailureHandlesGracefullyAndSavesFallbackMessage() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        
        AuthenticatedUser principal = new AuthenticatedUser(userEntity);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(userRepository.getReferenceById(userId)).thenReturn(userEntity);
        ChatSessionEntity newSession = new ChatSessionEntity();
        newSession.setId(UUID.randomUUID());
        when(chatSessionRepository.save(any())).thenReturn(newSession);

        when(restTemplate.exchange(
                any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Connection Refused"));

        ChatRequest request = new ChatRequest();
        request.setMessage("Fail me");

        ChatResponse response = chatService.chat(request, new MockHttpServletRequest());

        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessageEntity> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages.get(1).getSenderType()).isEqualTo("AGENT");
        assertThat(savedMessages.get(1).getContent()).contains("Xin lỗi, hiện tại tôi đang gặp khó khăn trong việc kết nối tới hệ thống AI");
        
        assertThat(response.answer()).contains("Xin lỗi, hiện tại tôi đang gặp khó khăn trong việc kết nối tới hệ thống AI");
    }
}
