package com.taller.services;

import com.taller.models.Cliente;
import com.taller.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar la recuperación de contraseñas de clientes.
 * Genera códigos temporales de 6 dígitos con expiración (15 minutos).
 * Si hay un servidor de correo configurado (SPRING_MAIL_HOST), envía el código por email;
 * de lo contrario, en modo desarrollo, lo imprime en la consola del servidor.
 */
@Service
public class RecuperacionPasswordService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:#{null}}")
    private String mailHost;

    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    // Almacenamiento en memoria de los códigos de verificación: email -> datos del código
    private final Map<String, CodigoRecuperacion> codigosPendientes = new ConcurrentHashMap<>();

    private static class CodigoRecuperacion {
        private final String codigo;
        private final LocalDateTime expiracion;

        public CodigoRecuperacion(String codigo, LocalDateTime expiracion) {
            this.codigo = codigo;
            this.expiracion = expiracion;
        }

        public boolean haExpirado() {
            return LocalDateTime.now().isAfter(expiracion);
        }

        public String getCodigo() {
            return codigo;
        }
    }

    /**
     * Paso 1: Solicitar código de recuperación.
     * Genera un código de 6 dígitos y lo envía por correo o lo imprime en consola.
     */
    public void solicitarCodigo(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Debes introducir un correo electrónico.");
        }

        String emailNormalizado = email.trim().toLowerCase();

        Cliente cliente = clienteRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna cuenta asociada a este correo electrónico."));

        if (!cliente.tieneCuenta()) {
            throw new IllegalArgumentException("Este cliente no tiene una cuenta registrada en la plataforma.");
        }

        // Generar código numérico de 6 dígitos (100000 - 999999)
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(15);

        codigosPendientes.put(emailNormalizado, new CodigoRecuperacion(codigo, expiracion));

        // Enviar por correo si está configurado
        if (mailSender != null && mailHost != null && !mailHost.isBlank()) {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                if (mailUsername != null && !mailUsername.isBlank()) {
                    mensaje.setFrom(mailUsername);
                }
                mensaje.setTo(emailNormalizado);
                mensaje.setSubject("Código de recuperación de contraseña - TallerPro");
                mensaje.setText("Hola " + cliente.getNombre() + ",\n\n"
                        + "Has solicitado restablecer tu contraseña en TallerPro.\n"
                        + "Tu código de verificación es: " + codigo + "\n\n"
                        + "Este código es válido durante 15 minutos.\n"
                        + "Si no has solicitado este cambio, puedes ignorar este mensaje.\n\n"
                        + "Saludos,\nEquipo de TallerPro");
                mailSender.send(mensaje);
            } catch (Exception e) {
                System.err.println("Error al enviar el correo de recuperación: " + e.getMessage());
                throw new IllegalStateException("No se pudo enviar el correo de recuperación. Inténtalo más tarde.");
            }
        } else {
            // Modo desarrollo: imprimir en consola
            System.out.println("=================================================");
            System.out.println("🔑 [MODO DESARROLLO] CÓDIGO DE RECUPERACIÓN DE CONTRASEÑA");
            System.out.println("Destinatario: " + emailNormalizado);
            System.out.println("Código: " + codigo);
            System.out.println("Caduca en: 15 minutos");
            System.out.println("=================================================");
        }
    }

    /**
     * Paso 2: Verificar que el código introducido es correcto y sigue vigente.
     */
    public void verificarCodigo(String email, String codigo) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Debes introducir el código de verificación.");
        }

        String emailNormalizado = email.trim().toLowerCase();
        CodigoRecuperacion info = codigosPendientes.get(emailNormalizado);

        if (info == null) {
            throw new IllegalArgumentException("No hay ninguna solicitud de recuperación activa para este correo.");
        }

        if (info.haExpirado()) {
            codigosPendientes.remove(emailNormalizado);
            throw new IllegalArgumentException("El código ha caducado. Por favor, solicita uno nuevo.");
        }

        if (!info.getCodigo().equals(codigo.trim())) {
            throw new IllegalArgumentException("El código introducido no es correcto.");
        }
    }

    /**
     * Paso 3: Guardar la nueva contraseña tras validar el código y los requisitos de seguridad.
     */
    public void restablecerPassword(String email, String codigo, String nuevaPassword, String confirmarPassword) {
        // Validar el código primero
        verificarCodigo(email, codigo);

        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
        }

        if (nuevaPassword.length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
        }

        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        String emailNormalizado = email.trim().toLowerCase();
        Cliente cliente = clienteRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));

        cliente.setPassword(nuevaPassword);
        clienteRepository.save(cliente);

        // Eliminar código para que no se pueda reutilizar
        codigosPendientes.remove(emailNormalizado);
    }
}
