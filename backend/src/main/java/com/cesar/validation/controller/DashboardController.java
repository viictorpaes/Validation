package com.cesar.validation.controller;

import com.cesar.validation.dto.DashboardStatsDto;
import com.cesar.validation.service.document.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores consolidados de comprovantes processados")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Retorna contadores de aprovação, reprovação, pendência e últimos envios")
    public ResponseEntity<DashboardStatsDto> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}
