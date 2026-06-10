package com.mangarec.features.chat.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ChatRequest")
public class ChatRequest {
    @NotBlank(message = "Message must not be blank")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    @Schema(example = "Recommend me a completed action manga with comedy.")
    private String message;
}
