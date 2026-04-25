package com.bharatpos.controller;

import com.bharatpos.dto.request.LoginRequest;
import com.bharatpos.dto.request.RegisterRequest;
import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.dto.response.AuthResponse;
import com.bharatpos.exception.BadRequestException;
import com.bharatpos.repository.UserRepository;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.security.JwtService;
import com.bharatpos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        String userId = jwtService.extractUserId(refreshToken);
        var user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new BadRequestException("User not found"));
        var tenant = tenantRepository.findById(user.getTenant().getId())
                .orElseThrow(() -> new BadRequestException("Tenant not found"));
        var store = user.getStore() != null
                ? storeRepository.findById(user.getStore().getId()).orElse(null) : null;

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), tenant.getId(),
                store != null ? store.getId() : null,
                user.getRole().name());

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("accessToken", newAccessToken)));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "BharatPOS API is running 🚀", null));
    }

//    @GetMapping("/me")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> me(
//            @RequestHeader("Authorization") String authHeader) {
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            throw new BadRequestException("No token");
//        }
//        String token = authHeader.substring(7);
//        String userId = jwtService.extractUserId(token);
//        var user = userRepository.findById(Long.parseLong(userId))
//                .orElseThrow(() -> new BadRequestException("User not found"));
//        var tenant = tenantRepository.findById(user.getTenant().getId()).orElse(null);
//        var store = user.getStore() != null
//                ? storeRepository.findById(user.getStore().getId()).orElse(null) : null;
//
//        return ResponseEntity.ok(ApiResponse.success(Map.of(
//                "id", user.getId(),
//                "name", user.getName(),
//                "phone", user.getPhone(),
//                "email", user.getEmail() != null ? user.getEmail() : "",
//                "role", user.getRole().name(),
//                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
//                "tenantId", tenant != null ? tenant.getId() : "",
//                "businessName", tenant != null ? tenant.getBusinessName() : "",
//                "plan", tenant != null ? tenant.getPlan() : "starter",
//                "storeId", store != null ? store.getId() : "",
//                "storeName", store != null ? store.getName() : "Main Branch"
//        )));
//    }
@GetMapping("/me")
public ResponseEntity<ApiResponse<Map<String, Object>>> me(
        @RequestHeader("Authorization") String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new BadRequestException("No token");
    }
    String token = authHeader.substring(7);
    String userId = jwtService.extractUserId(token);
    var user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new BadRequestException("User not found"));
    var tenant = tenantRepository.findById(user.getTenant().getId()).orElse(null);
    var store = user.getStore() != null
            ? storeRepository.findById(user.getStore().getId()).orElse(null) : null;

    Map<String, Object> responseData = new java.util.HashMap<>();
    responseData.put("id", user.getId());
    responseData.put("name", user.getName());
    responseData.put("phone", user.getPhone());
    responseData.put("email", user.getEmail() != null ? user.getEmail() : "");
    responseData.put("role", user.getRole().name());
    responseData.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
    responseData.put("tenantId", tenant != null ? tenant.getId() : "");
    responseData.put("businessName", tenant != null ? tenant.getBusinessName() : "");
    responseData.put("plan", tenant != null ? tenant.getPlan() : "starter");
    responseData.put("storeId", store != null ? store.getId() : "");
    responseData.put("storeName", store != null ? store.getName() : "Main Branch");

    return ResponseEntity.ok(ApiResponse.success(responseData));
}
}