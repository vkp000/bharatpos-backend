package com.bharatpos.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static BharatPOSDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof BharatPOSDetails) {
            return (BharatPOSDetails) auth.getDetails();
        }
        throw new RuntimeException("No authenticated user found");
    }

    public static Long getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static Long getCurrentStoreId() {
        return getCurrentUser().getStoreId();
    }
}