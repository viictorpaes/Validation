package com.cesar.validation.service.validation;

import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.enums.DocumentType;
import java.time.LocalDate;

public record ValidationContext(
    String ocrText,
    ExtractedDocumentDto extractedData,
    String informedHolderName,
    String informedCpf,
    DocumentType informedDocumentType,
    boolean aiSucceeded,
    LocalDate referenceDate
) {
}
