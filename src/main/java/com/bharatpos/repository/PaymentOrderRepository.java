package com.bharatpos.repository;

import com.bharatpos.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByRazorpayOrderId(String orderId);
    Optional<PaymentOrder> findByInvoiceNumber(String invoiceNumber);
    List<PaymentOrder> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<PaymentOrder> findByStatus(String status);
}