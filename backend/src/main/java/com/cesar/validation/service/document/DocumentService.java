package com.cesar.validation.service.document;

import com.cesar.validation.dto.DocumentSummaryDto;
import com.cesar.validation.dto.DocumentValidationResultDto;
import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.dto.ValidationRuleItemDto;
import com.cesar.validation.entity.DocumentSubmission;
import com.cesar.validation.enums.DocumentType;
import com.cesar.validation.enums.ProcessingStatus;
import com.cesar.validation.enums.ValidationStatus;
import com.cesar.validation.exception.InvalidDocumentFileException;
import com.cesar.validation.exception.ResourceNotFoundException;
import com.cesar.validation.repository.DocumentSubmissionRepository;
import com.cesar.validation.service.ai.DocumentAiInterpreter;
import com.cesar.validation.service.image.ImagePreprocessingService;
import com.cesar.validation.service.ocr.OcrExtractionService;
import com.cesar.validation.service.validation.ResidenceValidationService;
import com.cesar.validation.service.validation.ValidationContext;
import com.cesar.validation.util.PiiMaskingUtil;
import com.cesar.validation.util.TextNormalizationUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentSubmissionRepository repository;
    private final DocumentStorageService storageService;
    private final ImagePreprocessingService imagePreprocessingService;
    private final OcrExtractionService ocrExtractionService;
    private final DocumentAiInterpreter aiInterpreter;
    private final ResidenceValidationService validationService;
    private final ObjectMapper objectMapper;

    public DocumentService(
        DocumentSubmissionRepository repository,
        DocumentStorageService storageService,
        ImagePreprocessingService imagePreprocessingService,
        OcrExtractionService ocrExtractionService,
        DocumentAiInterpreter aiInterpreter,
        ResidenceValidationService validationService,
        ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.storageService = storageService;
        this.imagePreprocessingService = imagePreprocessingService;
        this.ocrExtractionService = ocrExtractionService;
        this.aiInterpreter = aiInterpreter;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DocumentValidationResultDto submitAndProcessDocument(
        MultipartFile file,
        DocumentType documentType,
        String holderName,
        String cpf
    ) {
        if (holderName == null || holderName.trim().length() < 3) {
            throw new InvalidDocumentFileException("Informe o nome completo do titular (mínimo de 3 caracteres).");
        }
        if (documentType == null) {
            throw new InvalidDocumentFileException("Selecione o tipo de comprovante de residência.");
        }

        UUID documentId = UUID.randomUUID();
        DocumentStorageService.StoredFileMetadata stored = storageService.validateAndStore(file, documentId);

        String cleanedCpf = TextNormalizationUtil.digitsOnly(cpf);
        DocumentSubmission submission = new DocumentSubmission();
        submission.setId(documentId);
        submission.setOriginalFileName(stored.originalFileName());
        submission.setStoredFilePath(stored.storedPath().toString());
        submission.setContentType(stored.contentType());
        submission.setFileSizeBytes(stored.fileSizeBytes());
        submission.setInformedHolderName(holderName.trim());
        submission.setInformedCpf(cleanedCpf.isEmpty() ? null : cleanedCpf);
        submission.setDocumentType(documentType);
        submission.setProcessingStatus(ProcessingStatus.RECEBIDO);
        submission.setValidationStatus(ValidationStatus.PENDENTE_ANALISE);
        submission.setOcrCharCount(0);
        submission.setCreatedAt(Instant.now());

        submission = repository.save(submission);
        log.info("Documento registrado para validação: id={}, tipo={}, bytes={}",
            documentId, documentType, stored.fileSizeBytes());

        try {
            // Etapa 2: OpenCV / Conversão de PDF
            submission.setProcessingStatus(ProcessingStatus.PROCESSANDO_IMAGEM);
            List<BufferedImage> processedImages = imagePreprocessingService.prepareDocumentImages(
                stored.storedPath(),
                stored.contentType()
            );

            // Etapa 3: Tesseract OCR
            submission.setProcessingStatus(ProcessingStatus.EXTRAINDO_TEXTO);
            String ocrText = ocrExtractionService.extractText(processedImages);
            submission.setOcrText(ocrText);
            submission.setOcrCharCount(ocrText != null ? ocrText.replaceAll("\\s+", "").length() : 0);

            // Etapa 4: Google Gemini IA
            submission.setProcessingStatus(ProcessingStatus.INTERPRETANDO_IA);
            DocumentAiInterpreter.AiInterpretationResult aiResult = aiInterpreter.interpret(ocrText, documentType);
            submission.setExtractedJson(serializeSafely(aiResult.data()));
            submission.setAiSource(aiResult.source());

            // Etapa 5: Regras de Negócio em Java
            submission.setProcessingStatus(ProcessingStatus.VALIDANDO_REGRAS);
            ValidationContext validationContext = new ValidationContext(
                ocrText,
                aiResult.data(),
                submission.getInformedHolderName(),
                submission.getInformedCpf(),
                documentType,
                aiResult.aiSucceeded(),
                LocalDate.now()
            );
            ResidenceValidationService.ValidationDecision decision = validationService.validate(validationContext);

            // Etapa 6: Persistência PostgreSQL
            submission.setValidationStatus(decision.status());
            submission.setSummaryMessage(decision.summaryMessage());
            submission.setReasonsJson(serializeSafely(decision.reasons()));
            submission.clearAndSetRuleResults(decision.ruleResults());
            submission.setProcessingStatus(ProcessingStatus.CONCLUIDO);
            submission.setProcessedAt(Instant.now());

            DocumentSubmission saved = repository.save(submission);
            log.info("Processamento concluído: id={}, status={}, fonteIA={}",
                saved.getId(), saved.getValidationStatus(), saved.getAiSource());

            return toValidationResultDto(saved);
        } catch (RuntimeException ex) {
            submission.setProcessingStatus(ProcessingStatus.ERRO);
            submission.setValidationStatus(ValidationStatus.PENDENTE_ANALISE);
            submission.setSummaryMessage("Falha técnica durante o processamento; documento pendente de análise.");
            submission.setReasonsJson(serializeSafely(List.of(ex.getMessage() != null
                ? ex.getMessage()
                : "Erro no pipeline de OCR/imagem.")));
            submission.setProcessedAt(Instant.now());
            repository.save(submission);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDto> listDocuments(ValidationStatus statusFilter) {
        List<DocumentSubmission> entities = statusFilter != null
            ? repository.findByValidationStatusOrderByCreatedAtDesc(statusFilter)
            : repository.findAllByOrderByCreatedAtDesc();

        return entities.stream()
            .map(this::toSummaryDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public DocumentValidationResultDto getDocumentDetails(UUID id) {
        DocumentSubmission entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comprovante não encontrado para o identificador informado."));
        return toValidationResultDto(entity);
    }

    @Transactional
    public void deleteDocument(UUID id) {
        DocumentSubmission entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comprovante não encontrado para remoção."));
        String storedPath = entity.getStoredFilePath();
        repository.delete(entity);
        storageService.deleteStoredFile(storedPath);
        log.info("Comprovante removido com sucesso: id={}", id);
    }

    public DocumentSummaryDto toSummaryDto(DocumentSubmission entity) {
        return new DocumentSummaryDto(
            entity.getId(),
            entity.getOriginalFileName(),
            entity.getContentType(),
            entity.getFileSizeBytes(),
            entity.getInformedHolderName(),
            entity.getDocumentType(),
            entity.getProcessingStatus(),
            entity.getValidationStatus(),
            entity.getSummaryMessage(),
            entity.getCreatedAt(),
            entity.getProcessedAt()
        );
    }

    private DocumentValidationResultDto toValidationResultDto(DocumentSubmission entity) {
        ExtractedDocumentDto extracted = deserializeExtracted(entity.getExtractedJson());
        if (extracted != null && extracted.cpf() != null) {
            extracted = new ExtractedDocumentDto(
                extracted.nomeTitular(),
                PiiMaskingUtil.maskCpf(extracted.cpf()),
                extracted.endereco(),
                extracted.cidade(),
                extracted.estado(),
                extracted.cep(),
                extracted.dataEmissao(),
                extracted.tipoDocumento(),
                extracted.camposAusentes()
            );
        }

        List<String> reasons = deserializeReasons(entity.getReasonsJson());
        List<ValidationRuleItemDto> rules = entity.getRuleResults().stream()
            .map(r -> new ValidationRuleItemDto(
                r.getRuleCode(),
                r.getRuleName(),
                r.getOutcome(),
                r.isPassed(),
                r.getMessage()
            ))
            .toList();

        return new DocumentValidationResultDto(
            entity.getId(),
            entity.getOriginalFileName(),
            entity.getContentType(),
            entity.getFileSizeBytes(),
            entity.getInformedHolderName(),
            PiiMaskingUtil.maskCpf(entity.getInformedCpf()),
            entity.getDocumentType(),
            entity.getProcessingStatus(),
            entity.getValidationStatus(),
            entity.getSummaryMessage(),
            reasons,
            extracted,
            rules,
            entity.getOcrCharCount(),
            PiiMaskingUtil.sanitizeOcrPreview(entity.getOcrText(), 320),
            entity.getAiSource(),
            entity.getCreatedAt(),
            entity.getProcessedAt()
        );
    }

    private String serializeSafely(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private ExtractedDocumentDto deserializeExtracted(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ExtractedDocumentDto.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<String> deserializeReasons(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
