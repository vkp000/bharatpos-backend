package com.bharatpos.controller;

import com.bharatpos.dto.request.CustomerRequest;
import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Customer;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers(
            @RequestParam(required = false) String q) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        List<Customer> customers = (q != null && !q.isBlank())
                ? customerService.searchCustomers(tenantId, q)
                : customerService.getAllCustomers(tenantId);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Customer customer = customerService.createCustomer(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customer));
    }
}