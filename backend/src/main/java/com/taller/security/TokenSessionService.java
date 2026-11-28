package com.taller.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio en memoria para gestionar tokens de sesión de usuarios y clientes autenticados.
 */
@Service
public class TokenSessionService {

    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60; // 8 horas
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessionInfo> sesiones = new ConcurrentHashMap<>();

    public record SessionInfo(Long userId, String email, String rol, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Crea una nueva sesión y devuelve el token generado.
     */
    public String crearSesion(Long userId, String email, String rol) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        sesiones.put(token, new SessionInfo(
                userId,
                email,
                rol != null ? rol.trim().toUpperCase() : "",
                Instant.now().plusSeconds(SESSION_TTL_SECONDS)
        ));
        return token;
    }

    /**
     * Obtiene la información de sesión asociada al token si es válida y no ha expirado.
     */
    public SessionInfo obtenerSesion(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        SessionInfo info = sesiones.get(token);
        if (info == null) {
            return null;
        }
        if (info.isExpired()) {
            sesiones.remove(token);
            return null;
        }
        return info;
    }

    /**
     * Invalida una sesión existente.
     */
    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }
}
