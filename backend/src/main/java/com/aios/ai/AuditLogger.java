package com.aios.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    @Async
    public void logInvocation(UUID conversationId,
                              String modelUsed,
                              long latencyMs,
                              Integer tokensUsed,
                              boolean fallbackUsed) {
        log.info(
                "ai_audit conversationId={} model={} latencyMs={} tokens={} fallback={}",
                conversationId,
                modelUsed,
                latencyMs,
                tokensUsed,
                fallbackUsed
        );
    }
}
