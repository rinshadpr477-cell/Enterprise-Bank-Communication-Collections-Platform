package com.ford.bank.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Authentication", description = "Development token generation endpoint")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.dev-mode", havingValue = "true")
public class DevAuthController {

    private final DevSecurityConfig devSecurityConfig;

    @Operation(summary = "Generate a mock development JWT token with selected roles")
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> generateToken(
            @RequestParam(defaultValue = "admin-user") String username,
            @RequestParam(defaultValue = "ADMIN,CAMPAIGN_MANAGER,COLLECTION_AGENT,AUDITOR") String roles) {

        List<String> roleList = List.of(roles.split(","));
        String token = devSecurityConfig.generateDevToken(username, roleList);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "token_type", "Bearer",
                "expires_in", 3600,
                "username", username,
                "roles", roleList
        ));
    }
}
