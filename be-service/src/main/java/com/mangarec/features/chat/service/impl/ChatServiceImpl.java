package com.mangarec.features.chat.service.impl;

import com.mangarec.features.chat.controller.request.ChatRequest;
import com.mangarec.features.chat.controller.response.ChatRecommendationResponse;
import com.mangarec.features.chat.controller.response.ChatResponse;
import com.mangarec.features.chat.service.ChatService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    @Override
    public ChatResponse mockChat(ChatRequest request) {
        return new ChatResponse(
                "Mock AI response for: " + request.getMessage(),
                List.of(
                        new ChatRecommendationResponse(
                                "mock-manga-001",
                                "Mock Manga Recommendation",
                                "Matched your requested genres, mood, and reading intent."
                        )
                ),
                Instant.now()
        );
    }
}
