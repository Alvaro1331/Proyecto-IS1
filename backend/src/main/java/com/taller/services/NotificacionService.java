package com.taller.services;

import com.taller.events.NotificacionReparacionEvent;
import com.taller.models.Cliente;
import com.taller.models.NotificacionCliente;
import com.taller.models.Reparacion;
import com.taller.models.TipoNotificacion;
import com.taller.repositories.NotificacionClienteRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de la gestión de notificaciones a clientes:
 * - Persistencia en base de datos para consulta en el Dashboard Cliente (campana 🔔).
 * - Envío de emails HTML informativos con enlaces de consulta directa.
 * - Simulación de avisos SMS/WhatsApp en consola.
 */
@Service
public class NotificacionService {

    @Autowired
    private NotificacionClienteRepository notificacionRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:#{null}}")
    private String mailHost;

    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    /**
     * Listener que reacciona a eventos de cambio de estado o incidencias de reparación.
     */
    @EventListener
    public void onNotificacionReparacion(NotificacionReparacionEvent event) {
        crearYProcesarNotificacion(
                event.getCliente(),
                event.getReparacion(),
                event.getTipo(),
                event.getTitulo(),
                event.getMensaje()
        );
    }

    /**
     * Registra una notificación en base de datos y dispara los canales de comunicación (Email y SMS).
     */
    @Transactional
    public NotificacionCliente crearYProcesarNotificacion(Cliente cliente, Reparacion reparacion,
                                                          TipoNotificacion tipo, String titulo, String mensaje) {
        if (cliente == null) {
            return null;
        }

        // 1. Guardar en base de datos para la campana del cliente
        NotificacionCliente notificacion = new NotificacionCliente(cliente, reparacion, tipo, titulo, mensaje);
        NotificacionCliente guardada = notificacionRepository.save(notificacion);

        // 2. Disparar canales externos de comunicación
        enviarEmailCliente(cliente, reparacion, titulo, mensaje);
        simularAvisoSms(cliente, reparacion, titulo, mensaje);

        return guardada;
    }

    /**
     * Envía un correo con diseño HTML al cliente o muestra el contenido por consola en modo desarrollo.
     */
    private void enviarEmailCliente(Cliente cliente, Reparacion reparacion, String titulo, String mensaje) {
        String email = cliente.getEmail();
        if (email == null || email.isBlank()) {
            return;
        }

        String localizador = reparacion != null ? reparacion.getLocalizador() : "";
        String matricula = (reparacion != null && reparacion.getVehiculo() != null)
                ? reparacion.getVehiculo().getMatricula() : "Vehículo";
        String marcaModelo = (reparacion != null && reparacion.getVehiculo() != null)
                ? reparacion.getVehiculo().getMarca() + " " + reparacion.getVehiculo().getModelo() : "";

        // Si hay servidor SMTP configurado, enviar correo HTML real
        if (mailSender != null && mailHost != null && !mailHost.isBlank()) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                if (mailUsername != null && !mailUsername.isBlank()) {
                    helper.setFrom(mailUsername);
                }
                helper.setTo(email);
                helper.setSubject("MotorFlow - " + titulo + " (" + matricula + ")");

                String htmlContent = construirPlantillaHtml(cliente.getNombre(), titulo, mensaje, localizador, matricula, marcaModelo);
                helper.setText(htmlContent, true);

                mailSender.send(mimeMessage);
                System.out.println("[EMAIL ENVIADO] A: " + email + " | Asunto: " + titulo);
            } catch (Exception e) {
                System.err.println("Error al enviar email de notificación: " + e.getMessage());
            }
        } else {
            // Modo desarrollo: Imprimir en consola de manera visual
            imprimirLogConsolaEmail(email, cliente.getNombre(), titulo, mensaje, localizador, matricula, marcaModelo);
        }
    }

    /**
     * Muestra en consola una simulación de SMS al teléfono registrado del cliente.
     */
    private void simularAvisoSms(Cliente cliente, Reparacion reparacion, String titulo, String mensaje) {
        String telefono = cliente.getTelefono();
        if (telefono == null || telefono.isBlank()) {
            return;
        }
        String localizador = reparacion != null ? reparacion.getLocalizador() : "";
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("[SMS/WHATSAPP SIMULADO] Destino: " + telefono + " (" + cliente.getNombre() + ")");
        System.out.println("Mensaje: MotorFlow: " + titulo + ". " + mensaje + (localizador.isEmpty() ? "" : " (Localizador: " + localizador + ")"));
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void imprimirLogConsolaEmail(String email, String nombre, String titulo, String mensaje,
                                         String localizador, String matricula, String marcaModelo) {
        System.out.println("================================================================================");
        System.out.println(" [NOTIFICACIÓN POR EMAIL - MODO DESARROLLO MOTORFLOW]");
        System.out.println(" Para: " + email + " (" + nombre + ")");
        System.out.println(" Asunto: MotorFlow - " + titulo + " [" + matricula + "]");
        System.out.println(" Vehículo: " + marcaModelo + " | Matrícula: " + matricula);
        if (localizador != null && !localizador.isBlank()) {
            System.out.println(" Localizador: " + localizador);
            System.out.println(" Enlace de consulta directa: http://localhost:8080/pages/consulta-estado.html?localizador=" + localizador);
        }
        System.out.println(" Contenido: " + mensaje);
        System.out.println("================================================================================");
    }

    private String construirPlantillaHtml(String nombreCliente, String titulo, String mensaje,
                                          String localizador, String matricula, String marcaModelo) {
        String urlConsulta = "http://localhost:8080/pages/consulta-estado.html" +
                (localizador != null && !localizador.isBlank() ? "?localizador=" + localizador : "");

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #1e1e1e; color: #f5f5f5; border: 1px solid #333; border-radius: 8px; overflow: hidden;\">"
                + "<div style=\"background-color: #e85d04; padding: 20px; text-align: center;\">"
                + "<h1 style=\"color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 1px;\">MOTORFLOW</h1>"
                + "<p style=\"color: #fff; margin: 5px 0 0 0; font-size: 14px;\">Sistema de Gestión de Taller</p>"
                + "</div>"
                + "<div style=\"padding: 24px 20px;\">"
                + "<h2 style=\"color: #ff7b29; margin-top: 0; font-size: 18px;\">" + titulo + "</h2>"
                + "<p style=\"font-size: 15px; color: #e2e8f0; line-height: 1.6;\">Hola <strong>" + nombreCliente + "</strong>,</p>"
                + "<p style=\"font-size: 15px; color: #cbd5e1; line-height: 1.6;\">" + mensaje + "</p>"
                + "<div style=\"background-color: #2b2b2b; border-left: 4px solid #e85d04; padding: 12px 16px; margin: 20px 0; border-radius: 4px;\">"
                + "<p style=\"margin: 0; color: #f5f5f5; font-size: 14px;\"><strong>Vehículo:</strong> " + marcaModelo + " (" + matricula + ")</p>"
                + (localizador != null && !localizador.isBlank() ? "<p style=\"margin: 6px 0 0 0; color: #f5f5f5; font-size: 14px;\"><strong>Localizador:</strong> <span style=\"background-color: #e85d04; color: #fff; padding: 2px 6px; border-radius: 4px; font-weight: bold;\">" + localizador + "</span></p>" : "")
                + "</div>"
                + "<div style=\"text-align: center; margin: 30px 0 10px 0;\">"
                + "<a href=\"" + urlConsulta + "\" style=\"display: inline-block; background-color: #e85d04; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: bold; font-size: 15px;\">Consultar Estado en Directo</a>"
                + "</div>"
                + "</div>"
                + "<div style=\"background-color: #151515; padding: 14px 20px; text-align: center; color: #888; font-size: 12px; border-top: 1px solid #2d2d2d;\">"
                + "<p style=\"margin: 0;\">Este es un mensaje automático de MotorFlow Taller Mecánico.</p>"
                + "</div>"
                + "</div>";
    }

    // --- Métodos de consulta y gestión para la API ---

    public List<NotificacionCliente> obtenerPorCliente(Long clienteId) {
        return notificacionRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId);
    }

    public long contarNoLeidas(Long clienteId) {
        return notificacionRepository.countByClienteIdAndLeidaFalse(clienteId);
    }

    @Transactional
    public NotificacionCliente marcarComoLeida(Long notificacionId) {
        NotificacionCliente notif = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada con ID: " + notificacionId));
        notif.setLeida(true);
        return notificacionRepository.save(notif);
    }

    @Transactional
    public void marcarTodasComoLeidas(Long clienteId) {
        List<NotificacionCliente> notificaciones = notificacionRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId);
        for (NotificacionCliente n : notificaciones) {
            if (!n.isLeida()) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        }
    }

    @Transactional
    public void eliminarNotificacion(Long notificacionId) {
        notificacionRepository.deleteById(notificacionId);
    }
}
