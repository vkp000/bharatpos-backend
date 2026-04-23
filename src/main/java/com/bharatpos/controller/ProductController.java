package com.bharatpos.controller;

import com.bharatpos.dto.request.ProductRequest;
import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Product;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(tenantId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Product>>> search(@RequestParam String q) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(tenantId, q)));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<Product>> getByBarcode(@PathVariable String barcode) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(productService.getByBarcode(tenantId, barcode)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long storeId = SecurityUtils.getCurrentStoreId();
        Product product = productService.createProduct(tenantId, storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Product product = productService.updateProduct(tenantId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        productService.deleteProduct(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }
}