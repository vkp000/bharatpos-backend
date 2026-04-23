package com.bharatpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    @Column(unique = true)
    private String gstin;

    private String pan;
    private String phone;
    private String email;
    private String address;
    private String state;
    private String pincode;
    private String logoUrl;

    @Column(nullable = false)
    @Builder.Default
    private String plan = "starter";

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Store> stores;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<User> users;
}