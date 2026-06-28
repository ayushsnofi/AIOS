package com.aios.chat.service;

import com.aios.ai.AIGatewayResult;
import com.aios.ai.AIGatewayService;
import com.aios.chat.dto.ChatExchangeResponse;
import com.aios.chat.dto.ConversationResponse;
import com.aios.chat.dto.CreateConversationRequest;
import com.aios.chat.dto.MessageResponse;
import com.aios.chat.dto.SendMessageRequest;
import com.aios.chat.entity.Conversation;
import com.aios.chat.entity.Message;
import com.aios.chat.entity.MessageRole;
import com.aios.chat.repository.ConversationRepository;
import com.aios.chat.repository.MessageRepository;
import com.aios.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIGatewayService aiGatewayService;

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "New Conversation");
        conversation.setModelUsed(request.getModel());
        Conversation saved = conversationRepository.save(conversation);
        return toConversationResponse(saved);
    }

    @Transactional
    public ChatExchangeResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        List<Message> history = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);

        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.getContent());
        messageRepository.save(userMessage);

        String modelHint = StringUtils.hasText(request.getModel())
                ? request.getModel()
                : conversation.getModelUsed();

        AIGatewayResult aiResult = aiGatewayService.execute(
                conversationId,
                request.getContent(),
                modelHint,
                history
        );

        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(aiResult.getContent());
        assistantMessage.setTokensUsed(aiResult.getTokensUsed());
        assistantMessage.setLatencyMs(aiResult.getLatencyMs());
        messageRepository.save(assistantMessage);

        conversation.setModelUsed(aiResult.getModelUsed());
        if (!StringUtils.hasText(conversation.getTitle()) || "New Conversation".equals(conversation.getTitle())) {
            conversation.setTitle(truncateTitle(request.getContent()));
        }
        conversationRepository.save(conversation);

        return ChatExchangeResponse.builder()
                .conversationId(conversationId)
                .userMessage(toMessageResponse(userMessage))
                .assistantMessage(toMessageResponse(assistantMessage))
                .modelUsed(aiResult.getModelUsed())
                .totalLatencyMs(aiResult.getLatencyMs())
                .build();
    }

    private String truncateTitle(String content) {
        String trimmed = content.strip();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 77) + "...";
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .modelUsed(conversation.getModelUsed())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().getValue())
                .content(message.getContent())
                .tokensUsed(message.getTokensUsed())
                .latencyMs(message.getLatencyMs())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
