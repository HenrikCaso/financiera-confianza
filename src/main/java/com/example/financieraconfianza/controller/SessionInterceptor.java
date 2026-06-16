package com.example.financieraconfianza.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();

        // Si el usuario NO está logueado en la sesión, lo mandamos de patitas al login
        if (session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect("/login");
            return false; // Corta el flujo por completo, la petición nunca llega al controlador
        }

        return true; // El usuario tiene sesión activa, lo dejamos pasar libremente
    }
}