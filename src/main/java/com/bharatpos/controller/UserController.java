package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Store;
import com.bharatpos.entity.Tenant;
import com.bharatpos.entity.User;
import com.bharatpos.enums.Role;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.repository.UserRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAll() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                userRepository.findByTenantId(tenantId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> create(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long storeId = SecurityUtils.getCurrentStoreId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

        String name = (String) body.get("name");
        String phone = (String) body.get("phone");
        String roleStr = (String) body.getOrDefault("role", "CASHIER");
        String password = (String) body.getOrDefault("password", "bharatpos123");

        if (userRepository.existsByPhone(phone)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Phone already registered"));
        }

        Role role;
        try { role = Role.valueOf(roleStr.toUpperCase()); }
        catch (Exception e) { role = Role.CASHIER; }

        User user = User.builder()
                .tenant(tenant)
                .store(store)
                .name(name)
                .phone(phone)
                .email((String) body.getOrDefault("email", ""))
                .password(passwordEncoder.encode(password))
                .role(role)
                .avatar(name != null ? name.substring(0, Math.min(2, name.length())).toUpperCase() : "BP")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff member created", userRepository.save(user)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<User>> toggleStatus(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setActive((Boolean) body.getOrDefault("active", !user.getActive()));
        return ResponseEntity.ok(ApiResponse.success("Status updated", userRepository.save(user)));
    }
}