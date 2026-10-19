package com.mangarec.features.chat.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

import java.time.Instant;
import java.util.List;

@Schema(name = "ChatResponse")
public record ChatResponse(
        @Schema(description = "ID of the chat session", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID sessionId,

        @Schema(example = "Mock AI response. Real AI integration will replace this later.")
        String answer,

        List<ChatRecommendationResponse> recommendations,

        Instant servedAt
) {
}
