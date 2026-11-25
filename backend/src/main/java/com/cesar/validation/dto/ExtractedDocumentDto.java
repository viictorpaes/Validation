package com.cesar.validation.dto;

import java.util.List;

public record ExtractedDocumentDto(
    String nomeTitular,
    String cpf,
    String endereco,
    String cidade,
    String estado,
    String cep,
    String dataEmissao,
    String tipoDocumento,
    List<String> camposAusentes
) {
    public ExtractedDocumentDto {
        camposAusentes = camposAusentes == null ? List.of() : List.copyOf(camposAusentes);
    }
}
