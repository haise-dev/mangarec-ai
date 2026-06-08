package com.mangarec.features.chat.controller;

import com.mangarec.common.response.ApiResponse;
import com.mangarec.features.chat.controller.request.ChatRequest;
import com.mangarec.features.chat.controller.response.ChatResponse;
import com.mangarec.features.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat")
public class ChatController {
    private final ChatService chatService;

    @Operation(summary = "Send chat message", description = "Mock AI response used to validate Redis rate limiting.")
    @PostMapping
    public ApiResponse<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        ChatResponse response = chatService.mockChat(request);
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Chat response generated")
                .data(response)
                .build();
    }
}
