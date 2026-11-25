package com.cesar.validation.service.validation;

import com.cesar.validation.enums.RuleOutcome;
import com.cesar.validation.util.TextNormalizationUtil;
import org.springframework.stereotype.Component;

@Component
public class CpfConsistencyRule implements ValidationRule {

    @Override
    public int order() {
        return 4;
    }

    @Override
    public String code() {
        return "CPF_CONSISTENCY";
    }

    @Override
    public String name() {
        return "Validação e Consistência de CPF";
    }

    @Override
    public RuleEvaluation evaluate(ValidationContext context) {
        String extractedCpf = context.extractedData() != null
            ? TextNormalizationUtil.digitsOnly(context.extractedData().cpf())
            : "";
        String informedCpf = TextNormalizationUtil.digitsOnly(context.informedCpf());

        if (!informedCpf.isEmpty() && !TextNormalizationUtil.isValidCpf(informedCpf)) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "O CPF informado no formulário é inválido."
            );
        }

        if (extractedCpf.isEmpty()) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.PASSED,
                "CPF não consta na fatura ou é opcional; sem inconsistências."
            );
        }

        if (!TextNormalizationUtil.isValidCpf(extractedCpf)) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "O CPF extraído do comprovante possui dígitos verificadores inválidos."
            );
        }

        if (!informedCpf.isEmpty() && !informedCpf.equals(extractedCpf)) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "O CPF extraído do comprovante diverge do CPF informado pelo usuário."
            );
        }

        return new RuleEvaluation(
            code(),
            name(),
            RuleOutcome.PASSED,
            "CPF válido e consistente no comprovante."
        );
    }
}
