package com.mangarec.features.chat.service;

import com.mangarec.features.chat.controller.request.ChatRequest;
import com.mangarec.features.chat.controller.response.ChatResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface ChatService {
    ChatResponse chat(ChatRequest request, HttpServletRequest httpRequest);
}
