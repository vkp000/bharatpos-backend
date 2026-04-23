package com.bharatpos.service;

import com.bharatpos.dto.request.LoginRequest;
import com.bharatpos.dto.request.RegisterRequest;
import com.bharatpos.dto.response.AuthResponse;
import com.bharatpos.entity.Store;
import com.bharatpos.entity.Tenant;
import com.bharatpos.entity.User;
import com.bharatpos.enums.Role;
import com.bharatpos.exception.BadRequestException;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.repository.UserRepository;
import com.bharatpos.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }

        // Create Tenant
        Tenant tenant = Tenant.builder()
                .businessName(request.getBusinessName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .gstin(request.getGstin())
                .state(request.getState())
                .plan("starter")
                .build();
        tenant = tenantRepository.save(tenant);

        // Create default Store
        Store store = Store.builder()
                .tenant(tenant)
                .name("Main Branch")
                .city(request.getState())
                .storeCode("MAIN-" + tenant.getId())
                .build();
        store = storeRepository.save(store);

        // Create Owner user
        User user = User.builder()
                .tenant(tenant)
                .store(store)
                .name(request.getOwnerName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.OWNER)
                .avatar(getAvatar(request.getOwnerName()))
                .build();
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), tenant.getId(), store.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return buildAuthResponse(user, tenant, store, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );

        User user = userRepository.findByPhoneOrEmail(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new BadRequestException("User not found"));

        Tenant tenant = user.getTenant();
        Store store = user.getStore();

        String accessToken = jwtService.generateAccessToken(
                user.getId(), tenant.getId(),
                store != null ? store.getId() : null,
                user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return buildAuthResponse(user, tenant, store, accessToken, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, Tenant tenant, Store store,
                                           String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .tenantId(tenant.getId())
                .storeId(store != null ? store.getId() : null)
                .businessName(tenant.getBusinessName())
                .plan(tenant.getPlan())
                .build();
    }

    private String getAvatar(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}