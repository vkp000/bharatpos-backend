package com.bharatpos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String toPhone;
    private String customerName;
    private String messageType; // INVOICE, LOYALTY, REMINDER, BROADCAST, OTP
    private String templateName;

    @Column(columnDefinition = "TEXT")
    private String messageBody;

    private String status; // sent, failed, delivered
    private String whatsappMessageId;
    private String errorMessage;
    private String referenceId; // sale ID, customer ID etc

    @CreationTimestamp
    private LocalDateTime sentAt;
}