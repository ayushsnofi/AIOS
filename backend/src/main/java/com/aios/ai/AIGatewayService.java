package com.aios.ai;

import com.aios.config.AiosProperties;
import com.aios.memory.MemoryService;
import com.aios.memory.RetrievedMemoryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIGatewayService {

    private final ChatModel chatModel;
    private final PromptFirewall promptFirewall;
    private final ModelRouter modelRouter;
    private final AuditLogger auditLogger;
    private final AiosProperties aiosProperties;
    private final MemoryService memoryService;
    private final MemoryContextPromptBuilder memoryContextPromptBuilder;

    public AIGatewayResult execute(UUID conversationId,
                                   String userPrompt,
                                   String requestedModel,
                                   List<com.aios.chat.entity.Message> history) {
        promptFirewall.validate(userPrompt);

        RetrievedMemoryContext memoryContext = memoryService.retrieveRelevantContext(
                userPrompt,
                aiosProperties.getMemory().getRetrievalLimit()
        );
        if (memoryContext.hasContext()) {
            log.debug("ai_gateway memory_context segments={} cached={}",
                    memoryContext.getSegmentIds().size(),
                    memoryContext.isFromSemanticCache());
        }

        String primaryModel = modelRouter.resolve(requestedModel);
        long startNanos = System.nanoTime();

        try {
            return invoke(conversationId, userPrompt, history, memoryContext, primaryModel, startNanos, false);
        } catch (Exception primaryFailure) {
            log.warn("Primary model {} failed, attempting fallback: {}", primaryModel, primaryFailure.getMessage());
            String fallbackModel = modelRouter.fallbackModel(primaryModel);
            AIGatewayResult result = invoke(conversationId, userPrompt, history, memoryContext, fallbackModel, startNanos, true);
            auditLogger.logInvocation(
                    conversationId,
                    result.getModelUsed(),
                    result.getLatencyMs(),
                    result.getTokensUsed(),
                    true
            );
            return result;
        }
    }

    private AIGatewayResult invoke(UUID conversationId,
                                   String userPrompt,
                                   List<com.aios.chat.entity.Message> history,
                                   RetrievedMemoryContext memoryContext,
                                   String model,
                                   long startNanos,
                                   boolean isRetry) {
        int maxRetries = aiosProperties.getAi().getMaxRetries();
        long retryDelayMs = aiosProperties.getAi().getRetryDelayMs();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    Thread.sleep(retryDelayMs * attempt);
                }
                List<Message> springMessages = buildSpringMessages(history, userPrompt, memoryContext);
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .model(model)
                        .build();
                Prompt prompt = new Prompt(springMessages, options);
                ChatResponse response = chatModel.call(prompt);

                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                String content = response.getResult().getOutput().getText();
                Integer tokensUsed = extractTokenUsage(response);

                if (!isRetry) {
                    auditLogger.logInvocation(conversationId, model, latencyMs, tokensUsed, false);
                }

                return AIGatewayResult.builder()
                        .content(content)
                        .modelUsed(model)
                        .tokensUsed(tokensUsed)
                        .latencyMs(latencyMs)
                        .build();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("AI gateway invocation interrupted", interrupted);
            } catch (Exception ex) {
                lastException = ex;
                log.debug("Model {} attempt {} failed: {}", model, attempt + 1, ex.getMessage());
            }
        }

        throw new IllegalStateException(
                "AI gateway failed after retries for model " + model,
                lastException
        );
    }

    private List<Message> buildSpringMessages(List<com.aios.chat.entity.Message> history,
                                              String userPrompt,
                                              RetrievedMemoryContext memoryContext) {
        List<Message> messages = new ArrayList<>();
        messages.add(memoryContextPromptBuilder.build(memoryContext));

        if (history != null) {
            for (com.aios.chat.entity.Message prior : history) {
                if (prior.getRole() == com.aios.chat.entity.MessageRole.SYSTEM) {
                    continue;
                }
                messages.add(toSpringMessage(prior));
            }
        }
        messages.add(new UserMessage(userPrompt));
        return messages;
    }

    private Message toSpringMessage(com.aios.chat.entity.Message entity) {
        return switch (entity.getRole()) {
            case USER -> new UserMessage(entity.getContent());
            case ASSISTANT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
        };
    }

    private Integer extractTokenUsage(ChatResponse response) {
        if (response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return null;
        }
        return (int) response.getMetadata().getUsage().getTotalTokens();
    }
}
