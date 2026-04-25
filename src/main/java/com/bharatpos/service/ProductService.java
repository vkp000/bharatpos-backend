package com.bharatpos.service;

import com.bharatpos.dto.request.ProductRequest;
import com.bharatpos.entity.Inventory;
import com.bharatpos.entity.Product;
import com.bharatpos.entity.Store;
import com.bharatpos.entity.Tenant;
import com.bharatpos.exception.BadRequestException;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.InventoryRepository;
import com.bharatpos.repository.ProductRepository;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;

    public List<Product> getAllProducts(Long tenantId) {
        return productRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    public List<Product> searchProducts(Long tenantId, String query) {
        if (query == null || query.isBlank()) return getAllProducts(tenantId);
        return productRepository.searchProducts(tenantId, query);
    }

    public Product getByBarcode(Long tenantId, String barcode) {
        return productRepository.findByBarcodeAndTenantId(barcode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with barcode: " + barcode));
    }

    @Transactional
    public Product createProduct(Long tenantId, Long storeId, ProductRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        String sku = (request.getSku() != null && !request.getSku().isBlank())
                ? request.getSku()
                : "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Ensure unique SKU
        while (productRepository.findBySkuAndTenantId(sku, tenantId).isPresent()) {
            sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Product product = Product.builder()
                .tenant(tenant)
                .name(request.getName().trim())
                .sku(sku)
                .barcode(request.getBarcode() != null ? request.getBarcode().trim() : null)
                .category(request.getCategory())
                .unit(request.getUnit() != null ? request.getUnit() : "PCS")
                .hsnCode(request.getHsnCode())
                .gstRate(request.getGstRate())
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .reorderLevel(request.getReorderLevel() != null ? request.getReorderLevel() : 10)
                .reorderQty(request.getReorderQty() != null ? request.getReorderQty() : 20)
                .supplierName(request.getSupplierName())
                .batchNumber(request.getBatchNumber())
                .expiryDate(request.getExpiryDate())
                .build();

        product = productRepository.save(product);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

        Inventory inventory = Inventory.builder()
                .product(product)
                .store(store)
                .quantity(request.getOpeningStock() != null ? request.getOpeningStock() : 0)
                .build();
        inventoryRepository.save(inventory);

        return product;
    }

    @Transactional
    public Product updateProduct(Long tenantId, Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (!product.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Access denied");
        }

        product.setName(request.getName().trim());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode().trim());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getUnit() != null) product.setUnit(request.getUnit());
        if (request.getHsnCode() != null) product.setHsnCode(request.getHsnCode());
        if (request.getGstRate() != null) product.setGstRate(request.getGstRate());
        if (request.getCostPrice() != null) product.setCostPrice(request.getCostPrice());
        if (request.getSellingPrice() != null) product.setSellingPrice(request.getSellingPrice());
        if (request.getReorderLevel() != null) product.setReorderLevel(request.getReorderLevel());
        if (request.getSupplierName() != null) product.setSupplierName(request.getSupplierName());

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long tenantId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (!product.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Access denied");
        }

        product.setActive(false);
        productRepository.save(product);
    }
}