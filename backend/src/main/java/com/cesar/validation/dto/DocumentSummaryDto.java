package com.cesar.validation.dto;

import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.enums.ProcessingStatus;
import com.cesar.validation.enums.ValidationStatus;
import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryDto(
    UUID id,
    String originalFileName,
    String contentType,
    long fileSizeBytes,
    String informedHolderName,
    DocumentType documentType,
    ProcessingStatus processingStatus,
    ValidationStatus validationStatus,
    String summaryMessage,
    Instant createdAt,
    Instant processedAt
) {
}
