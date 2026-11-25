package com.cesar.validation.service.document;

import com.cesar.validation.dto.DashboardStatsDto;
import com.cesar.validation.dto.DocumentSummaryDto;
import com.cesar.validation.enums.ValidationStatus;
import com.cesar.validation.repository.DocumentSubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DocumentSubmissionRepository repository;
    private final DocumentService documentService;

    public DashboardService(DocumentSubmissionRepository repository, DocumentService documentService) {
        this.repository = repository;
        this.documentService = documentService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long total = repository.count();
        long approved = repository.countByValidationStatus(ValidationStatus.APROVADO);
        long rejected = repository.countByValidationStatus(ValidationStatus.REPROVADO);
        long pending = repository.countByValidationStatus(ValidationStatus.PENDENTE_ANALISE);

        List<DocumentSummaryDto> recent = repository.findTop5ByOrderByCreatedAtDesc().stream()
            .map(documentService::toSummaryDto)
            .toList();

        return new DashboardStatsDto(total, approved, rejected, pending, recent);
    }
}
