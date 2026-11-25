package com.cesar.validation.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponseDto(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<String> details
) {
}
