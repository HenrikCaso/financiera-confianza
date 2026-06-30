package com.example.financieraconfianza.config;

// Aquí importamos el interceptor desde tu carpeta controller
import com.example.financieraconfianza.controller.SessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor; // El que ya tenías para JWT

    @Autowired
    private SessionInterceptor sessionInterceptor; // Nuestro nuevo guardián de sesiones

    // ¡Un solo método para gobernarlos a todos!
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. Registramos tu interceptor JWT antiguo
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/login", "/registro", "/", "/academia", "/productos/**");

        // 2. Registramos el NUEVO interceptor de Sesión en el mismo método
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns(
                        "/dashboard/**",
                        "/transferencias/**",
                        "/servicios/**",
                        "/tarjetas/**",
                        "/prestamos/**",
                        "/perfil/**"
                )
                .excludePathPatterns(
                        "/login",
                        "/registro",
                        "/css/**",
                        "/js/**",
                        "/img/**"
                );
    }
}
