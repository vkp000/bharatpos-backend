package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Supplier;
import com.bharatpos.entity.Tenant;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.SupplierRepository;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Supplier>>> getAll() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                supplierRepository.findByTenantIdAndActiveTrue(tenantId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> create(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Supplier supplier = Supplier.builder()
                .tenant(tenant)
                .name((String) body.get("name"))
                .contactPerson((String) body.get("contactPerson"))
                .phone((String) body.get("phone"))
                .email((String) body.get("email"))
                .gstin((String) body.get("gstin"))
                .category((String) body.get("category"))
                .paymentTerms((String) body.get("paymentTerms"))
                .address((String) body.get("address"))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created", supplierRepository.save(supplier)));
    }
}