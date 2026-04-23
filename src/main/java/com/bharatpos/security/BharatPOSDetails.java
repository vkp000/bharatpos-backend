package com.bharatpos.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BharatPOSDetails {
    private Long userId;
    private Long tenantId;
    private Long storeId;
    private String role;
}