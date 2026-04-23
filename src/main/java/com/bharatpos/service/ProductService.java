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
        return productRepository.searchProducts(tenantId, query);
    }

    public Product getByBarcode(Long tenantId, String barcode) {
        return productRepository.findByBarcodeAndTenantId(barcode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
    }

    @Transactional
    public Product createProduct(Long tenantId, Long storeId, ProductRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        // Generate SKU if not provided
        String sku = request.getSku() != null ? request.getSku()
                : "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Product product = Product.builder()
                .tenant(tenant)
                .name(request.getName())
                .sku(sku)
                .barcode(request.getBarcode())
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

        // Create inventory record for the store
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
            throw new BadRequestException("Product does not belong to this tenant");
        }

        product.setName(request.getName());
        product.setBarcode(request.getBarcode());
        product.setCategory(request.getCategory());
        product.setHsnCode(request.getHsnCode());
        product.setGstRate(request.getGstRate());
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setReorderLevel(request.getReorderLevel());
        product.setReorderQty(request.getReorderQty());

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long tenantId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (!product.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Product does not belong to this tenant");
        }

        product.setActive(false);
        productRepository.save(product);
    }
}