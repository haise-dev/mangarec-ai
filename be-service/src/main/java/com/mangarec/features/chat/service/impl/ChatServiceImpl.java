package com.mangarec.features.chat.service.impl;

import com.mangarec.features.chat.controller.request.ChatRequest;
import com.mangarec.features.chat.controller.response.ChatRecommendationResponse;
import com.mangarec.features.chat.controller.response.ChatResponse;
import com.mangarec.features.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RestTemplate restTemplate;

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    @Override
    public ChatResponse mockChat(ChatRequest request) {
        String endpoint = aiServiceUrl + "/api/v1/chat/";
        
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
            
            String answer = (String) body.getOrDefault("answer", "No answer provided.");
            List<Map<String, Object>> mangas = (List<Map<String, Object>>) body.getOrDefault("mangas", Collections.emptyList());
            
            List<ChatRecommendationResponse> recommendations = mangas.stream().map(m -> {
                String mangaId = (String) m.getOrDefault("manga_id", "unknown-id");
                String title = (String) m.getOrDefault("title", "Unknown Title");
                String reason = (String) m.getOrDefault("description", "Recommended based on your query.");
                return new ChatRecommendationResponse(mangaId, title, reason);
            }).collect(Collectors.toList());
            
            return new ChatResponse(answer, recommendations, Instant.now());
            
        } catch (Exception e) {
            log.error("Failed to communicate with AI service: {}", e.getMessage(), e);
            // Fallback to error response
            return new ChatResponse(
                    "Xin lỗi, hiện tại tôi đang gặp khó khăn trong việc kết nối tới hệ thống AI. Vui lòng thử lại sau.",
                    Collections.emptyList(),
                    Instant.now()
            );
        }
    }
}
