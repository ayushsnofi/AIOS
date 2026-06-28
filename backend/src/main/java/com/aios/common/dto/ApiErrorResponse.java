package com.aios.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private boolean success;
    private String message;
    private String errorCode;
    private List<String> details;
    private Instant timestamp;

    public static ApiErrorResponse of(String message, String errorCode) {
        return ApiErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .build();
    }
}
