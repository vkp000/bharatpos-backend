package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Store;
import com.bharatpos.entity.Tenant;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Store>>> getAll() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                storeRepository.findByTenantId(tenantId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Store>> create(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        String name = (String) body.get("name");
        String city = (String) body.getOrDefault("city", "");
        String storeCode = "STR-" + tenantId + "-" + System.currentTimeMillis();

        Store store = Store.builder()
                .tenant(tenant)
                .name(name)
                .city(city)
                .address((String) body.getOrDefault("address", ""))
                .managerName((String) body.getOrDefault("managerName", ""))
                .phone((String) body.getOrDefault("phone", ""))
                .gstin((String) body.getOrDefault("gstin", ""))
                .storeCode(storeCode)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store created", storeRepository.save(store)));
    }
}