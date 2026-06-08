package com.mangarec.features.chat.service;

import com.mangarec.features.chat.controller.request.ChatRequest;
import com.mangarec.features.chat.controller.response.ChatResponse;

public interface ChatService {
    ChatResponse mockChat(ChatRequest request);
}
