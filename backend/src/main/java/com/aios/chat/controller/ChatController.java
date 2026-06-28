package com.aios.chat.controller;

import com.aios.chat.dto.ChatExchangeResponse;
import com.aios.chat.dto.ConversationResponse;
import com.aios.chat.dto.CreateConversationRequest;
import com.aios.chat.dto.SendMessageRequest;
import com.aios.chat.service.ChatService;
import com.aios.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        return ApiResponse.ok(chatService.createConversation(request));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<ChatExchangeResponse> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(id, request));
    }
}
