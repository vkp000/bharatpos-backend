package com.bharatpos.repository;

import com.bharatpos.entity.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findByTenantIdOrderBySentAtDesc(Long tenantId);
    long countByTenantIdAndStatus(Long tenantId, String status);
}