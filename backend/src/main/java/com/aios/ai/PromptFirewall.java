package com.aios.ai;

import com.aios.common.exception.PromptFirewallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PromptFirewall {

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior)\\s+instructions"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(dan|jailbreak)"),
            Pattern.compile("(?i)disregard\\s+(your\\s+)?(system|safety)\\s+(prompt|rules)"),
            Pattern.compile("(?i)reveal\\s+(the\\s+)?(system\\s+)?prompt"),
            Pattern.compile("(?i)\\bDAN\\s+mode\\b"),
            Pattern.compile("(?i)override\\s+security\\s+policy")
    );

    private static final int MAX_PROMPT_LENGTH = 32_000;

    public void validate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new PromptFirewallException("Prompt must not be empty");
        }
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new PromptFirewallException("Prompt exceeds maximum allowed length");
        }
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                log.warn("Prompt firewall blocked request: pattern={}", pattern.pattern());
                throw new PromptFirewallException("Prompt rejected by security policy");
            }
        }
    }
}
