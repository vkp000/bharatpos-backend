package com.bharatpos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer quantityBefore;
    private Integer quantityAfter;
    private Integer change;

    @Column(length = 100)
    private String reason;

    @Column(length = 50)
    private String type; // SALE, PURCHASE, ADJUSTMENT, DAMAGE, EXPIRY

    @CreationTimestamp
    private LocalDateTime createdAt;
}