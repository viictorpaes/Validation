package com.cesar.validation.service.validation;

import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.enums.RuleOutcome;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeConsistencyRule implements ValidationRule {

    private static final Map<DocumentType, String> TYPE_LABELS = Map.of(
        DocumentType.CONTA_DE_AGUA, "Conta de água",
        DocumentType.CONTA_DE_ENERGIA, "Conta de energia elétrica",
        DocumentType.CONTA_DE_TELEFONE, "Conta de telefone / internet",
        DocumentType.FATURA_CARTAO, "Fatura de cartão de crédito",
        DocumentType.CONTRATO_ALUGUEL, "Contrato de aluguel",
        DocumentType.OUTROS, "Outros comprovantes"
    );

    @Override
    public int order() {
        return 2;
    }

    @Override
    public String code() {
        return "DOCUMENT_TYPE_MATCH";
    }

    @Override
    public String name() {
        return "Compatibilidade do Tipo de Documento";
    }

    @Override
    public RuleEvaluation evaluate(ValidationContext context) {
        DocumentType informed = context.informedDocumentType();
        String rawExtracted = context.extractedData() != null
            ? context.extractedData().tipoDocumento()
            : null;

        if (rawExtracted == null || rawExtracted.isBlank()) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.PASSED,
                "Nenhum indício de categoria conflitante identificado no comprovante."
            );
        }

        DocumentType extractedType = parseExtractedType(rawExtracted);
        if (extractedType == null || extractedType == DocumentType.OUTROS) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.PASSED,
                "Comprovante genérico compatível com a validação de residência."
            );
        }

        if (informed != null && informed != extractedType) {
            String informedLabel = TYPE_LABELS.getOrDefault(informed, informed.name());
            String extractedLabel = TYPE_LABELS.getOrDefault(extractedType, extractedType.name());
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "Tipo de documento incompatível: foi selecionado '" + informedLabel
                    + "', mas o arquivo enviado corresponde a '" + extractedLabel + "'."
            );
        }

        String matchedLabel = TYPE_LABELS.getOrDefault(extractedType, extractedType.name());
        return new RuleEvaluation(
            code(),
            name(),
            RuleOutcome.PASSED,
            "Tipo de documento compatível com a opção selecionada (" + matchedLabel + ")."
        );
    }

    private DocumentType parseExtractedType(String raw) {
        String normalized = raw.trim().toUpperCase();
        try {
            return DocumentType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            if (normalized.contains("AGUA") || normalized.contains("ÁGUA") || normalized.contains("SANEAMENTO")) {
                return DocumentType.CONTA_DE_AGUA;
            }
            if (normalized.contains("ENERGIA") || normalized.contains("LUZ") || normalized.contains("ELETRICA")) {
                return DocumentType.CONTA_DE_ENERGIA;
            }
            if (normalized.contains("TELEFONE") || normalized.contains("INTERNET") || normalized.contains("TELECOM")) {
                return DocumentType.CONTA_DE_TELEFONE;
            }
            if (normalized.contains("CARTAO") || normalized.contains("CARTÃO")) {
                return DocumentType.FATURA_CARTAO;
            }
            if (normalized.contains("ALUGUEL") || normalized.contains("LOCACAO") || normalized.contains("LOCAÇÃO")) {
                return DocumentType.CONTRATO_ALUGUEL;
            }
            return null;
        }
    }
}
