package com.cesar.validation.dto;

import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.enums.ProcessingStatus;
import com.cesar.validation.enums.ValidationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentValidationResultDto(
    UUID id,
    String originalFileName,
    String contentType,
    long fileSizeBytes,
    String informedHolderName,
    String informedCpf,
    DocumentType documentType,
    ProcessingStatus processingStatus,
    ValidationStatus validationStatus,
    String summaryMessage,
    List<String> reasons,
    ExtractedDocumentDto extractedData,
    List<ValidationRuleItemDto> appliedRules,
    int ocrCharacterCount,
    String ocrPreview,
    String aiSource,
    Instant createdAt,
    Instant processedAt
) {
}
