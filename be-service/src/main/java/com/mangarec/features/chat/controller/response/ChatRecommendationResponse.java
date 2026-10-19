package com.mangarec.features.chat.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ChatRecommendationResponse")
public record ChatRecommendationResponse(
        @Schema(example = "mock-manga-001")
        String mangaDexId,

        @Schema(example = "Mock Manga Recommendation")
        String title,

        @Schema(example = "Matched your requested genres and tone.")
        String reason
) {
}
