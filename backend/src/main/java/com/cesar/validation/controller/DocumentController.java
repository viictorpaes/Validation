package com.cesar.validation.controller;

import com.cesar.validation.dto.DocumentSummaryDto;
import com.cesar.validation.dto.DocumentValidationResultDto;
import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.enums.ValidationStatus;
import com.cesar.validation.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documentos", description = "Envio, consulta e validação inteligente de comprovantes de residência")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia um comprovante de residência e executa o pipeline completo (OpenCV, OCR, IA e Regras)")
    public ResponseEntity<DocumentValidationResultDto> uploadDocument(
        @RequestPart("file") MultipartFile file,
        @RequestParam("documentType") DocumentType documentType,
        @RequestParam("holderName") String holderName,
        @RequestParam(value = "cpf", required = false) String cpf
    ) {
        DocumentValidationResultDto result = documentService.submitAndProcessDocument(
            file,
            documentType,
            holderName,
            cpf
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Operation(summary = "Lista todos os comprovantes enviados, com filtro opcional por status de validação")
    public ResponseEntity<List<DocumentSummaryDto>> listDocuments(
        @RequestParam(value = "status", required = false) ValidationStatus status
    ) {
        return ResponseEntity.ok(documentService.listDocuments(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna os detalhes completos de um documento específico")
    public ResponseEntity<DocumentValidationResultDto> getDocument(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getDocumentDetails(id));
    }

    @GetMapping("/{id}/result")
    @Operation(summary = "Retorna o parecer detalhado da validação e regras aplicadas de um documento")
    public ResponseEntity<DocumentValidationResultDto> getDocumentResult(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getDocumentDetails(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um comprovante e seu arquivo armazenado, respeitando a LGPD")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
