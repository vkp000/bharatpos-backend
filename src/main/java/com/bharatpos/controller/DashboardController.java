package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.dto.response.DashboardResponse;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        DashboardResponse response = dashboardService.getDashboard(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}