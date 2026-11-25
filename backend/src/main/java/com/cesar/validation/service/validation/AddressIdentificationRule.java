package com.cesar.validation.service.validation;

import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.enums.RuleOutcome;
import org.springframework.stereotype.Component;

@Component
public class AddressIdentificationRule implements ValidationRule {

    @Override
    public int order() {
        return 5;
    }

    @Override
    public String code() {
        return "ADDRESS_IDENTIFIED";
    }

    @Override
    public String name() {
        return "Identificação do Endereço Residencial";
    }

    @Override
    public RuleEvaluation evaluate(ValidationContext context) {
        ExtractedDocumentDto data = context.extractedData();
        String address = data != null ? data.endereco() : null;
        String city = data != null ? data.cidade() : null;
        String state = data != null ? data.estado() : null;

        if (address == null || address.trim().length() < 5) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.INCONCLUSIVE,
                "Endereço residencial não pôde ser identificado com segurança no comprovante."
            );
        }

        boolean hasLocation = (city != null && !city.isBlank()) || (state != null && !state.isBlank());
        if (!hasLocation) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.INCONCLUSIVE,
                "Logradouro localizado, mas cidade ou estado não foram identificados."
            );
        }

        return new RuleEvaluation(
            code(),
            name(),
            RuleOutcome.PASSED,
            "Endereço residencial e localidade identificados com sucesso."
        );
    }
}
