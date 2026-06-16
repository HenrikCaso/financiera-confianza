package com.example.financieraconfianza.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("tokenJwt");

        // 1. SI INTENTAN ENTRAR AL CORE BANCARIO (/admin/**)
        if (uri.startsWith("/admin") && !uri.equals("/admin/login")) {
            if (token == null || !jwtUtil.validarToken(token)) {
                response.sendRedirect("/admin/login");
                return false;
            }

            String rol = jwtUtil.obtenerRolDeToken(token);
            if (!"ADMIN".equals(rol)) {
                // BLINDAJE: Si un cliente intenta infiltrarse, lanzamos un 403 Prohibido
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado. No tienes rol de Administrador.");
                return false;
            }
        }

        // 2. SI INTENTAN ENTRAR AL HOMEBANKING CLIENTE (/dashboard, /transferencias, /prestamos, /servicios)
        if (uri.equals("/dashboard") || uri.equals("/transferencias") || uri.equals("/prestamos") || uri.equals("/servicios") || uri.equals("/perfil")) {
            if (token == null || !jwtUtil.validarToken(token)) {
                response.sendRedirect("/login");
                return false;
            }

            String rol = jwtUtil.obtenerRolDeToken(token);
            if (!"CLIENTE".equals(rol)) {
                response.sendRedirect("/admin/login"); // Si un admin se confunde de portal, lo mandamos a su login
                return false;
            }
        }

        return true; // Si pasa las pruebas, se le permite el acceso
    }
}