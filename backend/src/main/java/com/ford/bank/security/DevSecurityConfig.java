package com.ford.bank.security;

import com.ford.bank.kafka.consumer.ProcessedEvent;
import com.ford.bank.kafka.consumer.ProcessedEventRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Development-only JWT configuration.
 * Generates an ephemeral RSA key pair at startup for local testing.
 *
 * WARNING: This configuration must NEVER be used in production.
 * Set SECURITY_DEV_MODE=false and configure a real OAuth2 provider.
 *
 * This class also provides a utility method to generate test tokens for
 * use in development and automated tests.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.security.dev-mode", havingValue = "true")
public class DevSecurityConfig {

    private RSAKey rsaKey;

    @Bean
    @Primary
    public JwtDecoder devJwtDecoder() throws Exception {
        log.warn("=============================================================");
        log.warn("DEV MODE: Using ephemeral RSA key for JWT validation.");
        log.warn("This is NOT suitable for production use.");
        log.warn("Set app.security.dev-mode=false in production.");
        log.warn("=============================================================");

        this.rsaKey = new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();

        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) rsaKey.toPublicKey()).build();
    }

    /**
     * Generate a dev JWT token for a given subject and roles.
     * Used in integration tests and local development.
     */
    public String generateDevToken(String subject, List<String> roles) {
        try {
            JWSSigner signer = new RSASSASigner(rsaKey);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("bank-comm-dev")
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("roles", roles)
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dev token", e);
        }
    }
}
