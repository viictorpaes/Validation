package com.cesar.validation.service.validation;

import com.cesar.validation.entity.DocumentRuleResult;
import com.cesar.validation.enums.RuleOutcome;
import com.cesar.validation.enums.ValidationStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ResidenceValidationService {

    private final List<ValidationRule> orderedRules;

    public ResidenceValidationService(List<ValidationRule> rules) {
        this.orderedRules = rules.stream()
            .sorted(Comparator.comparingInt(ValidationRule::order))
            .toList();
    }

    public ValidationDecision validate(ValidationContext context) {
        List<DocumentRuleResult> ruleEntities = new ArrayList<>(orderedRules.size());
        List<String> failedMessages = new ArrayList<>();
        List<String> inconclusiveMessages = new ArrayList<>();

        int idx = 1;
        for (ValidationRule rule : orderedRules) {
            ValidationRule.RuleEvaluation eval = rule.evaluate(context);
            DocumentRuleResult entity = new DocumentRuleResult(
                UUID.randomUUID(),
                idx++,
                eval.code(),
                eval.name(),
                eval.outcome(),
                eval.message()
            );
            ruleEntities.add(entity);

            if (eval.outcome() == RuleOutcome.FAILED) {
                failedMessages.add(eval.message());
            } else if (eval.outcome() == RuleOutcome.INCONCLUSIVE) {
                inconclusiveMessages.add(eval.message());
            }
        }

        if (!failedMessages.isEmpty()) {
            return new ValidationDecision(
                ValidationStatus.REPROVADO,
                "Comprovante reprovado pelas regras de negócio.",
                failedMessages,
                ruleEntities
            );
        }

        if (!inconclusiveMessages.isEmpty()) {
            return new ValidationDecision(
                ValidationStatus.PENDENTE_ANALISE,
                "Comprovante pendente de análise manual por informações insuficientes.",
                inconclusiveMessages,
                ruleEntities
            );
        }

        return new ValidationDecision(
            ValidationStatus.APROVADO,
            "Comprovante de residência validado e aprovado com sucesso.",
            List.of("Todas as regras de legibilidade, titularidade, endereço e prazo foram atendidas."),
            ruleEntities
        );
    }

    public record ValidationDecision(
        ValidationStatus status,
        String summaryMessage,
        List<String> reasons,
        List<DocumentRuleResult> ruleResults
    ) {
    }
}
