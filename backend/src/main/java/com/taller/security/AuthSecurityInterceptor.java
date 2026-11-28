package com.taller.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/**
 * Interceptor que valida el token Bearer y los roles requeridos en los endpoints anotados con @RequiereRol.
 */
@Component
public class AuthSecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenSessionService tokenSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiereRol requiereRol = handlerMethod.getMethodAnnotation(RequiereRol.class);
        if (requiereRol == null) {
            requiereRol = handlerMethod.getBeanType().getAnnotation(RequiereRol.class);
        }

        if (requiereRol == null) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            escribirErrorJson(response, HttpServletResponse.SC_UNAUTHORIZED, "No autorizado. Token ausente.");
            return false;
        }

        String token = authHeader.substring(7).trim();
        TokenSessionService.SessionInfo sesion = tokenSessionService.obtenerSesion(token);
        if (sesion == null) {
            escribirErrorJson(response, HttpServletResponse.SC_UNAUTHORIZED, "No autorizado. Token inválido o expirado.");
            return false;
        }

        boolean rolPermitido = Arrays.stream(requiereRol.value())
                .anyMatch(rol -> rol != null && rol.equalsIgnoreCase(sesion.rol()));

        if (!rolPermitido) {
            escribirErrorJson(response, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado. No tienes permisos suficientes.");
            return false;
        }

        return true;
    }

    private void escribirErrorJson(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje.replace("\"", "\\\"") + "\"}");
    }
}
