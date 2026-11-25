package com.cesar.validation.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.enums.RuleOutcome;
import com.cesar.validation.enums.ValidationStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResidenceValidationServiceTest {

    // CPF sintético matematicamente válido apenas para testes automatizados (sem pessoa real)
    private static final String VALID_SYNTHETIC_CPF = "52998224725";
    private static final String SECOND_VALID_SYNTHETIC_CPF = "11144477735";

    private ResidenceValidationService validationService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getValidation().setMaxDocumentAgeDays(90);
        props.getValidation().setMinOcrChars(25);
        props.getValidation().setNameSimilarityThreshold(0.60);

        List<ValidationRule> rules = List.of(
            new LegibilityValidationRule(props),
            new DocumentTypeConsistencyRule(),
            new HolderNameMatchRule(props),
            new CpfConsistencyRule(),
            new AddressIdentificationRule(),
            new IssueDateValidityRule(props)
        );
        validationService = new ResidenceValidationService(rules);
    }

    @Test
    @DisplayName("Deve aprovar comprovante válido, legível, titular correspondente e emitido nos últimos 90 dias")
    void shouldApproveValidRecentDocument() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "MARIA CLARA OLIVEIRA TESTE",
            VALID_SYNTHETIC_CPF,
            "Rua Projetada Alfa, 150, Apto 202",
            "Recife",
            "PE",
            "50000000",
            today.minusDays(20).toString(),
            "CONTA_DE_ENERGIA",
            List.of()
        );

        ValidationContext ctx = new ValidationContext(
            "CONTA DE ENERGIA ELETRICA TITULAR MARIA CLARA OLIVEIRA TESTE RUA PROJETADA ALFA 150 RECIFE PE",
            extracted,
            "Maria Clara Oliveira Teste",
            VALID_SYNTHETIC_CPF,
            DocumentType.CONTA_DE_ENERGIA,
            true,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.APROVADO, decision.status());
        assertEquals(6, decision.ruleResults().size());
        assertTrue(decision.ruleResults().stream().allMatch(r -> r.getOutcome() == RuleOutcome.PASSED));
    }

    @Test
    @DisplayName("Deve reprovar quando usuário seleciona Conta de Água mas envia Conta de Energia")
    void shouldRejectMismatchedDocumentType() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "Maria Clara Oliveira Teste",
            VALID_SYNTHETIC_CPF,
            "Rua Projetada Alfa, 150",
            "Recife",
            "PE",
            "50000000",
            today.minusDays(10).toString(),
            "CONTA_DE_ENERGIA",
            List.of()
        );

        ValidationContext ctx = new ValidationContext(
            "NEOENERGIA CONTA DE LUZ MARIA CLARA OLIVEIRA TESTE RUA PROJETADA ALFA 150 RECIFE PE",
            extracted,
            "Maria Clara Oliveira Teste",
            VALID_SYNTHETIC_CPF,
            DocumentType.CONTA_DE_AGUA,
            true,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.REPROVADO, decision.status());
        assertTrue(decision.reasons().stream().anyMatch(m -> m.contains("Tipo de documento incompatível")));
    }

    @Test
    @DisplayName("Deve reprovar documento ilegível com texto OCR insuficiente")
    void shouldRejectIllegibleDocument() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            null, null, null, null, null, null, null, null, List.of("nomeTitular")
        );

        ValidationContext ctx = new ValidationContext(
            "abc 123",
            extracted,
            "Maria Clara Oliveira Teste",
            null,
            DocumentType.CONTA_DE_AGUA,
            false,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.REPROVADO, decision.status());
        assertTrue(decision.reasons().stream().anyMatch(m -> m.toLowerCase().contains("ilegível")));
    }

    @Test
    @DisplayName("Deve reprovar quando o nome do titular extraído diverge do informado pelo usuário")
    void shouldRejectMismatchedHolderName() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "Carlos Eduardo Ferreira Ficticio",
            null,
            "Avenida Central Beta, 400",
            "Recife",
            "PE",
            "50010000",
            today.minusDays(15).toString(),
            "CONTA_DE_AGUA",
            List.of("cpf")
        );

        ValidationContext ctx = new ValidationContext(
            "COMPANHIA DE SANEAMENTO CONTA DE AGUA CARLOS EDUARDO FERREIRA FICTICIO AVENIDA CENTRAL BETA 400",
            extracted,
            "Maria Clara Oliveira Teste",
            null,
            DocumentType.CONTA_DE_AGUA,
            true,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.REPROVADO, decision.status());
        assertTrue(decision.reasons().stream().anyMatch(m -> m.toLowerCase().contains("titular")));
    }

    @Test
    @DisplayName("Deve reprovar comprovante com data de emissão superior a 90 dias")
    void shouldRejectExpiredDocument() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "Maria Clara Oliveira Teste",
            null,
            "Rua Projetada Alfa, 150",
            "Recife",
            "PE",
            null,
            today.minusDays(125).toString(),
            "CONTA_DE_TELEFONE",
            List.of()
        );

        ValidationContext ctx = new ValidationContext(
            "FATURA TELEFONE MARIA CLARA OLIVEIRA TESTE RUA PROJETADA ALFA 150 RECIFE PE EMISSAO ANTIGA",
            extracted,
            "Maria Clara Oliveira Teste",
            null,
            DocumentType.CONTA_DE_TELEFONE,
            true,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.REPROVADO, decision.status());
        assertTrue(decision.reasons().stream().anyMatch(m -> m.contains("90 dias")));
    }

    @Test
    @DisplayName("Deve reprovar quando CPF extraído diverge do CPF informado pelo usuário")
    void shouldRejectConflictingCpf() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "Maria Clara Oliveira Teste",
            SECOND_VALID_SYNTHETIC_CPF,
            "Rua Projetada Alfa, 150",
            "Recife",
            "PE",
            null,
            today.minusDays(10).toString(),
            "CONTA_DE_ENERGIA",
            List.of()
        );

        ValidationContext ctx = new ValidationContext(
            "CONTA DE ENERGIA MARIA CLARA OLIVEIRA TESTE RUA PROJETADA ALFA 150 RECIFE PE",
            extracted,
            "Maria Clara Oliveira Teste",
            VALID_SYNTHETIC_CPF,
            DocumentType.CONTA_DE_ENERGIA,
            true,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.REPROVADO, decision.status());
        assertFalse(decision.reasons().isEmpty());
    }

    @Test
    @DisplayName("Deve classificar como PENDENTE_ANALISE quando documento é legível mas faltam campos essenciais")
    void shouldMarkPendingWhenDateOrAddressMissing() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        ExtractedDocumentDto extracted = new ExtractedDocumentDto(
            "Maria Clara Oliveira Teste",
            null,
            "Rua Projetada Alfa, 150",
            "Recife",
            "PE",
            null,
            null, // data de emissão ausente
            "CONTA_DE_AGUA",
            List.of("dataEmissao")
        );

        ValidationContext ctx = new ValidationContext(
            "CONTA DE AGUA MARIA CLARA OLIVEIRA TESTE RUA PROJETADA ALFA 150 RECIFE PE SEM DATA VISIVEL",
            extracted,
            "Maria Clara Oliveira Teste",
            null,
            DocumentType.CONTA_DE_AGUA,
            true,
            today
        );

        ResidenceValidationService.ValidationDecision decision = validationService.validate(ctx);

        assertEquals(ValidationStatus.PENDENTE_ANALISE, decision.status());
        assertTrue(decision.reasons().stream().anyMatch(m -> m.toLowerCase().contains("data de emissão")));
    }
}
