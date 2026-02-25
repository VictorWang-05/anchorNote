package com.csci310.anchornotes.security;

import com.csci310.anchornotes.config.SupabaseConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseJwtVerifier {

    private final SupabaseConfig supabaseConfig;

    public Claims verifyToken(String token) {
        try {
            // Supabase JWTs are signed with the JWT secret from your project settings
            // NOT the service role key - get this from: Settings → API → JWT Settings → JWT Secret
            byte[] keyBytes = supabaseConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(keyBytes)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims;
        } catch (Exception e) {
            log.error("Failed to verify Supabase JWT token", e);
            return null;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = verifyToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getEmailFromToken(String token) {
        Claims claims = verifyToken(token);
        return claims != null ? (String) claims.get("email") : null;
    }

    public boolean validateToken(String token) {
        return verifyToken(token) != null;
    }
}
