package com.example.financieraconfianza.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    private static final List<String> ROLES_CORPORATIVOS = List.of(
            "ADMIN",
            "ASESOR",
            "RIESGOS",
            "OPERACIONES",
            "RECUPERACIONES",
            "AUDITOR"
    );

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);

        if (uri.equals("/admin/login")) {
            return true;
        }

        if (uri.startsWith("/admin")) {

            if (session == null) {
                response.sendRedirect("/admin/login");
                return false;
            }

            String token = (String) session.getAttribute("tokenJwt");

            if (token == null || !jwtUtil.validarToken(token)) {
                response.sendRedirect("/admin/login");
                return false;
            }

            String rol = jwtUtil.obtenerRolDeToken(token);

            rol = rol == null ? "" : rol.trim().toUpperCase();

            if (!ROLES_CORPORATIVOS.contains(rol)) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Acceso denegado. No tienes rol corporativo."
                );
                return false;
            }

            return true;
        }

        if (
                uri.equals("/dashboard")
                        || uri.equals("/transferencias")
                        || uri.equals("/prestamos")
                        || uri.equals("/servicios")
                        || uri.equals("/perfil")
        ) {

            if (session == null) {
                response.sendRedirect("/login");
                return false;
            }

            String token = (String) session.getAttribute("tokenJwt");

            if (token == null || !jwtUtil.validarToken(token)) {
                response.sendRedirect("/login");
                return false;
            }

            String rol = jwtUtil.obtenerRolDeToken(token);

            rol = rol == null ? "" : rol.trim().toUpperCase();

            if (!"CLIENTE".equals(rol)) {
                response.sendRedirect("/admin/login");
                return false;
            }
        }

        return true;
    }
}