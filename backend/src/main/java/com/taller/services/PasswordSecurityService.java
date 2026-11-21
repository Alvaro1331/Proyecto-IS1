package com.taller.services;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * Servicio centralizado para validación de fortaleza mínima y hash criptográfico
 * irreversible de contraseñas mediante BCrypt (con salt aleatorio único por usuario).
 */
@Service
public class PasswordSecurityService {

    public static final int LONGITUD_MINIMA = 8;
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    /**
     * Valida que la contraseña nueva cumpla la política mínima de seguridad:
     * al menos 8 caracteres combinando letras y números.
     */
    public void validarFortaleza(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        if (password.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres y combinar letras y números.");
        }
        boolean tieneLetra = false;
        boolean tieneNumero = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                tieneLetra = true;
            } else if (Character.isDigit(c)) {
                tieneNumero = true;
            }
        }
        if (!tieneLetra || !tieneNumero) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres y combinar letras y números.");
        }
    }

    /**
     * Comprueba si una cadena almacenada ya corresponde a un hash BCrypt válido.
     */
    public boolean estaHasheada(String valor) {
        return valor != null && BCRYPT_PATTERN.matcher(valor).matches();
    }

    /**
     * Genera un hash BCrypt con salt aleatorio. Si ya está hasheada, evita el doble hasheo.
     */
    public String hashear(String passwordPlana) {
        if (passwordPlana == null || passwordPlana.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        if (estaHasheada(passwordPlana)) {
            return passwordPlana;
        }
        return encoder.encode(passwordPlana);
    }

    /**
     * Verifica la contraseña introducida frente al hash BCrypt almacenado
     * (manteniendo compatibilidad en tiempo constante frente a claves heredadas antes de migrar).
     */
    public boolean coincide(String passwordPlana, String passwordGuardada) {
        if (passwordPlana == null || passwordGuardada == null || passwordGuardada.isEmpty()) {
            return false;
        }
        if (estaHasheada(passwordGuardada)) {
            return encoder.matches(passwordPlana, passwordGuardada);
        }
        return MessageDigest.isEqual(
                passwordPlana.getBytes(StandardCharsets.UTF_8),
                passwordGuardada.getBytes(StandardCharsets.UTF_8)
        );
    }
}
