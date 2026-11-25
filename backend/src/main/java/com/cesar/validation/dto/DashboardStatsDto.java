package com.cesar.validation.dto;

import java.util.List;

public record DashboardStatsDto(
    long totalDocuments,
    long approvedDocuments,
    long rejectedDocuments,
    long pendingDocuments,
    List<DocumentSummaryDto> recentDocuments
) {
}
