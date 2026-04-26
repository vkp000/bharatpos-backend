package com.bharatpos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    @Builder.Default
    private String plan = "starter";

    @Column(nullable = false)
    @Builder.Default
    private String status = "active"; // active, expired, cancelled

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate trialEndDate;

    private String razorpaySubscriptionId;
    private String razorpayCustomerId;

    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyAmount;

    @Builder.Default
    private Integer invoiceCount = 0;

    @Builder.Default
    private Integer maxInvoices = 500;

    @Builder.Default
    private Integer maxStores = 1;

    @Builder.Default
    private Integer maxUsers = 1;

    @CreationTimestamp
    private LocalDateTime createdAt;
}