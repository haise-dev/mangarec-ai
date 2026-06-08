package com.mangarec.features.chat.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ChatResponse")
public record ChatResponse(
        @Schema(example = "Mock AI response. Real AI integration will replace this later.")
        String answer,

        List<ChatRecommendationResponse> recommendations,

        Instant servedAt
) {
}
