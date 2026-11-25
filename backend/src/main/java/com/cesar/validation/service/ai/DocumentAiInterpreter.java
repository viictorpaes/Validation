package com.cesar.validation.service.ai;

import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.enums.DocumentType;

public interface DocumentAiInterpreter {

    AiInterpretationResult interpret(String ocrText, DocumentType informedDocumentType);

    record AiInterpretationResult(
        ExtractedDocumentDto data,
        String source,
        boolean aiSucceeded
    ) {
    }
}
