package com.bharatpos.dto.response;

import com.bharatpos.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private Role role;
    private Long tenantId;
    private Long storeId;
    private String businessName;
    private String plan;
}