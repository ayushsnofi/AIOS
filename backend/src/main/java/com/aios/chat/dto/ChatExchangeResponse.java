package com.aios.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatExchangeResponse {

    private UUID conversationId;
    private MessageResponse userMessage;
    private MessageResponse assistantMessage;
    private String modelUsed;
    private Long totalLatencyMs;
}
