package com.mangarec.features.chat.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@Schema(name = "ChatRequest")
public class ChatRequest {
    @Schema(description = "Optional session ID for continuing conversation")
    private UUID sessionId;

    @NotBlank(message = "Message must not be blank")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    @Schema(example = "Suggest me a romantic manga.")
    private String message;
}
