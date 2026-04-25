package com.bharatpos.service;

import com.bharatpos.dto.request.CustomerRequest;
import com.bharatpos.entity.Customer;
import com.bharatpos.entity.Tenant;
import com.bharatpos.exception.BadRequestException;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.CustomerRepository;
import com.bharatpos.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    public List<Customer> getAllCustomers(Long tenantId) {
        return customerRepository.findByTenantId(tenantId);
    }

    public List<Customer> searchCustomers(Long tenantId, String query) {
        if (query == null || query.isBlank()) return getAllCustomers(tenantId);
        return customerRepository.searchCustomers(tenantId, query);
    }

    public Customer getById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    @Transactional
    public Customer createCustomer(Long tenantId, CustomerRequest request) {
        customerRepository.findByPhoneAndTenantId(request.getPhone(), tenantId)
                .ifPresent(c -> {
                    throw new BadRequestException(
                            "Customer with phone " + request.getPhone() + " already exists");
                });

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Customer customer = Customer.builder()
                .tenant(tenant)
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail())
                .address(request.getAddress())
                .gstin(request.getGstin())
                .segment("new")
                .build();

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long customerId, CustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        customer.setName(request.getName().trim());
        if (request.getEmail() != null) customer.setEmail(request.getEmail());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getGstin() != null) customer.setGstin(request.getGstin());
        return customerRepository.save(customer);
    }
}