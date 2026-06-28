package com.aios.ai;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AIGatewayResult {

    String content;
    String modelUsed;
    Integer tokensUsed;
    long latencyMs;
}
